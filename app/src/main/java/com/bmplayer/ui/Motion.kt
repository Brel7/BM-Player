package com.bmplayer.ui

import androidx.compose.runtime.Composable
import com.bmplayer.ui.theme.LocalAnimationScale
import kotlin.math.roundToInt

fun animationScale(speed: String): Float = when (speed) {
    "slow" -> 1.6f
    "fast" -> 0.6f
    "off" -> 0f
    else -> 1f
}

// Media3 default (C.DEFAULT_MAX_SEEK_TO_PREVIOUS_POSITION_MS): past 3 s, Previous restarts the current track.
fun previousRestartsTrack(positionMs: Long): Boolean = positionMs > 3_000L

fun scaledDuration(baseMs: Int, scale: Float): Int = (baseMs * scale).roundToInt().coerceAtLeast(0)

@Composable
fun motionMs(baseMs: Int): Int = scaledDuration(baseMs, LocalAnimationScale.current)
