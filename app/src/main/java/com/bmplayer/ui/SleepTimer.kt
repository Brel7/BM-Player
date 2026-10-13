package com.bmplayer.ui

fun sleepTimerProgress(remainingMs: Long, totalMs: Long): Float =
    if (totalMs <= 0L) 0f else (remainingMs.toFloat() / totalMs).coerceIn(0f, 1f)

fun formatTimerRemaining(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return if (h > 0) "$h:${m.toString().padStart(2, '0')}:$s" else "$m:$s"
}
