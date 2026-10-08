package com.bmplayer.playback

import android.os.Bundle
import android.app.PendingIntent
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.common.Player
import androidx.glance.appwidget.updateAll
import com.google.common.util.concurrent.Futures
import com.bmplayer.widget.BMPlayerWidget
import com.bmplayer.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioEffects: AudioEffectsManager? = null
    private var crossfadeMs = 5_000L
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeInProgress = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true
        ).setHandleAudioBecomingNoisy(true).build()
        audioEffects = AudioEffectsManager(player.audioSessionId)
        player.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                val metadata = mediaItem?.mediaMetadata
                getSharedPreferences("bm_player_widget", MODE_PRIVATE).edit()
                    .putString("title", metadata?.title?.toString().orEmpty())
                    .putString("artist", metadata?.artist?.toString().orEmpty())
                    .apply()
                serviceScope.launch { BMPlayerWidget().updateAll(this@PlaybackService) }
            }
        })
        fadeRunnable = object : Runnable {
            override fun run() {
                if (!crossfadeInProgress && player.repeatMode != Player.REPEAT_MODE_ONE && crossfadeMs > 0 && player.duration > 0 && player.nextMediaItemIndex != C.INDEX_UNSET && player.duration - player.currentPosition <= crossfadeMs) startCrossfade(player)
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
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        serviceScope.cancel()
        fadeRunnable?.let(fadeHandler::removeCallbacks)
        fadeRunnable = null
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
                    player.pause()
                    player.seekTo(nextIndex, position)
                    player.volume = 1f
                    player.play()
                    secondary.stop()
                    secondary.release()
                    crossfadePlayer = null
                    crossfadeInProgress = false
                }
            }
        }
        fadeHandler.post(fade)
    }
}
