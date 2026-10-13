package com.bmplayer.widget

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.bmplayer.playback.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

class WidgetControlCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val command = parameters[COMMAND] ?: return
        // Glance passes the BroadcastReceiver's restricted context, which may not bind to services; the app context can.
        val appContext = context.applicationContext
        withContext(Dispatchers.Main) {
            val future = MediaController.Builder(appContext, SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))).buildAsync()
            val controller = runCatching { withContext(Dispatchers.IO) { future.get(5, TimeUnit.SECONDS) } }
                .onFailure { Log.w("BMPlayback", "widget: connexion au lecteur impossible", it) }
                .getOrNull()
            if (controller == null) {
                MediaController.releaseFuture(future)
                return@withContext
            }
            try {
                // An empty player means the process died: play() makes the service restore the saved queue (onPlaybackResumption).
                val empty = controller.mediaItemCount == 0
                when {
                    command == "toggle" && controller.isPlaying -> controller.pause()
                    command == "previous" && !empty -> controller.seekToPrevious()
                    command == "next" && !empty -> controller.seekToNext()
                    else -> {
                        if (controller.playbackState == Player.STATE_IDLE && !empty) controller.prepare()
                        controller.play()
                        // Stay bound until playback starts so the freshly started service can promote itself to foreground.
                        withTimeoutOrNull(3_000L) { while (!controller.isPlaying) delay(100L) }
                    }
                }
            } finally {
                controller.release()
            }
        }
    }

    companion object {
        val COMMAND = ActionParameters.Key<String>("command")
    }
}
