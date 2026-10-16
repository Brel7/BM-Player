package com.bmplayer.playback

import android.os.Bundle
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.common.Player
import androidx.glance.appwidget.updateAll
import com.google.common.util.concurrent.Futures
import com.bmplayer.widget.BMPlayerWidget
import com.bmplayer.widget.WidgetState
import com.bmplayer.data.preferences.UserPreferencesStore
import com.bmplayer.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import com.bmplayer.ui.dominantAccent
import com.bmplayer.ui.loadArtworkBitmap
import android.graphics.Bitmap
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import com.bmplayer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    @Inject lateinit var repository: MusicRepository
    @Inject lateinit var preferencesStore: UserPreferencesStore
    private var mediaSession: MediaSession? = null
    private var audioEffects: AudioEffectsManager? = null
    private var crossfadeMs = 5_000L
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeInProgress = false
    private var crossfadeRunnable: Runnable? = null
    private var consecutiveErrors = 0
    private var widgetJob: Job? = null
    private var widgetArtworkUri: android.net.Uri? = null
    private var widgetArtwork: Bitmap? = null
    private var widgetAccent: Int? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true
        ).setHandleAudioBecomingNoisy(true).setWakeMode(C.WAKE_MODE_LOCAL).build()
        audioEffects = AudioEffectsManager(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying && crossfadeInProgress) cancelCrossfade(player)
                publishWidget(player)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                // reason: 2 = audio focus loss, 3 = audio becoming noisy, 5 = end of media item
                if (!playWhenReady) Log.i(TAG, "pause, raison=$reason")
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.w(TAG, "erreur de lecture", error)
                consecutiveErrors++
                // No play(): playWhenReady is kept, so a paused player stays paused after the skip.
                if (shouldSkipAfterError(consecutiveErrors, player.mediaItemCount, player.hasNextMediaItem())) {
                    player.seekToNextMediaItem()
                    player.prepare()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) consecutiveErrors = 0
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                publishWidget(player)
            }
        })
        object : Runnable {
            override fun run() {
                if (shouldStartCrossfade(player.isPlaying, crossfadeInProgress, player.repeatMode == Player.REPEAT_MODE_ONE, crossfadeMs, player.duration, player.currentPosition, player.hasNextMediaItem())) startCrossfade(player)
                fadeHandler.postDelayed(this, 250L)
            }
        }.also { fadeHandler.post(it) }
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(object : MediaSession.Callback {
                // Media3 rejects custom commands a controller was not granted here; only this app's own controllers get them.
                override fun onConnect(session: MediaSession, controller: MediaSession.ControllerInfo): MediaSession.ConnectionResult {
                    if (controller.packageName != packageName) return super.onConnect(session, controller)
                    val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().apply {
                        PlaybackCommands.ALL.forEach { add(SessionCommand(it, Bundle.EMPTY)) }
                    }.build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session).setAvailableSessionCommands(commands).build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ) = when (customCommand.customAction) {
                    PlaybackCommands.SET_EFFECTS_ENABLED -> {
                        if (audioEffects?.hasAvailableEffects != true) {
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                        } else {
                            audioEffects?.setEnabled(args.getBoolean(PlaybackCommands.ENABLED))
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    PlaybackCommands.SET_CROSSFADE -> {
                        crossfadeMs = args.getInt(PlaybackCommands.SECONDS).coerceIn(0, 15) * 1_000L
                        if (crossfadeMs == 0L) player.volume = 1f
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    PlaybackCommands.SET_BASS_STRENGTH -> {
                        audioEffects?.setBassStrength(args.getInt(PlaybackCommands.STRENGTH).toShort())
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    PlaybackCommands.SET_VIRTUALIZER_STRENGTH -> {
                        audioEffects?.setVirtualizerStrength(args.getInt(PlaybackCommands.STRENGTH).toShort())
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    PlaybackCommands.PLAY_NEXT -> {
                        val track = repository.tracks.value.firstOrNull { it.id == args.getLong(PlaybackCommands.TRACK_ID) }
                        if (track == null) {
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                        } else {
                            val index = args.getInt(PlaybackCommands.INDEX).coerceIn(0, player.mediaItemCount)
                            player.addMediaItem(index, track.toMediaItem())
                            if (player.shuffleModeEnabled && index > 0) {
                                // Media3 drops a new item at a random shuffled spot; put it right after the item it follows.
                                val timeline = player.currentTimeline
                                val order = generateSequence(timeline.getFirstWindowIndex(true).takeIf { it != C.INDEX_UNSET }) { i ->
                                    timeline.getNextWindowIndex(i, Player.REPEAT_MODE_OFF, true).takeIf { it != C.INDEX_UNSET }
                                }.toList()
                                player.setShuffleOrder(DefaultShuffleOrder(moveAfter(order, index, index - 1).toIntArray(), System.nanoTime()))
                            }
                            Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                        }
                    }
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }

                // Media3 calls this when a controller (the widget) sends play() to an empty player, e.g. after process death.
                override fun onPlaybackResumption(mediaSession: MediaSession, controller: MediaSession.ControllerInfo): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
                    val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
                    serviceScope.launch {
                        val saved = preferencesStore.preferences.first()
                        if (repository.tracks.value.isEmpty()) runCatching { withContext(Dispatchers.IO) { repository.refresh() } }
                        val plan = planResume(saved.playbackQueueIds, saved.playbackTrackId, saved.playbackPositionMs, repository.tracks.value.associateBy { it.id })
                        if (plan == null) {
                            future.setException(IllegalStateException("No saved queue to resume"))
                        } else {
                            player.shuffleModeEnabled = saved.shuffleEnabled
                            player.repeatMode = saved.repeatMode
                            future.set(MediaSession.MediaItemsWithStartPosition(plan.items.map { it.toMediaItem() }, plan.startIndex, plan.startPositionMs))
                        }
                    }
                    return future
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        fadeHandler.removeCallbacksAndMessages(null)
        crossfadePlayer?.release()
        crossfadePlayer = null
        mediaSession?.run {
            player.release()
            release()
        }
        audioEffects?.release()
        audioEffects = null
        mediaSession = null
        super.onDestroy()
    }

    private fun startCrossfade(player: ExoPlayer) {
        val nextIndex = player.nextMediaItemIndex
        if (nextIndex == androidx.media3.common.C.INDEX_UNSET || crossfadeMs <= 0L) return
        crossfadeInProgress = true
        val next = player.getMediaItemAt(nextIndex)
        val secondary = ExoPlayer.Builder(this).setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false
        ).build().also { crossfadePlayer = it }
        secondary.setMediaItem(next)
        secondary.volume = 0f
        secondary.prepare()
        secondary.play()
        val start = android.os.SystemClock.uptimeMillis()
        val fade = object : Runnable {
            override fun run() {
                val progress = ((android.os.SystemClock.uptimeMillis() - start).toFloat() / crossfadeMs).coerceIn(0f, 1f)
                player.volume = 1f - progress
                secondary.volume = progress
                if (progress < 1f) {
                    fadeHandler.postDelayed(this, 50L)
                } else {
                    val position = secondary.currentPosition
                    // Clear state before pause() so onIsPlayingChanged does not treat this handover as a user pause.
                    crossfadeRunnable = null
                    crossfadePlayer = null
                    crossfadeInProgress = false
                    player.pause()
                    player.seekTo(nextIndex, position)
                    player.volume = 1f
                    player.play()
                    secondary.stop()
                    secondary.release()
                }
            }
        }
        crossfadeRunnable = fade
        fadeHandler.post(fade)
    }

    private fun publishWidget(player: ExoPlayer) {
        val item = player.currentMediaItem
        val title = item?.mediaMetadata?.title?.toString()
        val artist = item?.mediaMetadata?.artist?.toString().orEmpty()
        val trackUri = item?.localConfiguration?.uri
        val artworkUri = item?.mediaMetadata?.artworkUri
        val isPlaying = player.isPlaying
        widgetJob?.cancel() // a slow artwork load for an old track must never overwrite the new one
        widgetJob = serviceScope.launch {
            if (trackUri != widgetArtworkUri) {
                widgetArtwork = trackUri?.let { loadArtworkBitmap(this@PlaybackService, it, artworkUri, 256) }
                widgetAccent = withContext(Dispatchers.IO) { widgetArtwork?.let(::dominantAccent) }
                widgetArtworkUri = trackUri
            }
            withContext(Dispatchers.IO) { WidgetState.save(this@PlaybackService, title, artist, isPlaying, widgetAccent, widgetArtwork) }
            BMPlayerWidget().updateAll(this@PlaybackService)
        }
    }

    private fun cancelCrossfade(player: ExoPlayer) {
        crossfadeRunnable?.let(fadeHandler::removeCallbacks)
        crossfadeRunnable = null
        crossfadePlayer?.release()
        crossfadePlayer = null
        player.volume = 1f
        crossfadeInProgress = false
    }

    private companion object {
        const val TAG = "BMPlayback"
    }
}
