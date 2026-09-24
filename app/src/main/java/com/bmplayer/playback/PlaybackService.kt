package com.bmplayer.playback

import android.os.Bundle
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
import com.google.common.util.concurrent.Futures

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var audioEffects: AudioEffectsManager? = null
    private var crossfadeMs = 5_000L
    private val fadeHandler = Handler(Looper.getMainLooper())
    private var fadeRunnable: Runnable? = null
    private var crossfadePlayer: ExoPlayer? = null
    private var crossfadeInProgress = false

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true
        ).setHandleAudioBecomingNoisy(true).build()
        audioEffects = AudioEffectsManager(player.audioSessionId)
        fadeRunnable = object : Runnable {
            override fun run() {
                if (!crossfadeInProgress && crossfadeMs > 0 && player.duration > 0 && player.nextMediaItemIndex != androidx.media3.common.C.INDEX_UNSET && player.duration - player.currentPosition <= crossfadeMs) startCrossfade(player)
                fadeHandler.postDelayed(this, 250L)
            }
        }.also { fadeHandler.post(it) }
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(object : MediaSession.Callback {
                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: SessionCommand,
                    args: Bundle
                ) = when (customCommand.customAction) {
                    PlaybackCommands.SET_EFFECTS_ENABLED -> {
                        audioEffects?.setEnabled(args.getBoolean(PlaybackCommands.ENABLED))
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    PlaybackCommands.SET_CROSSFADE -> {
                        crossfadeMs = args.getInt(PlaybackCommands.SECONDS).coerceIn(0, 15) * 1_000L
                        if (crossfadeMs == 0L) player.volume = 1f
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                    else -> Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
                }
            })
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
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
