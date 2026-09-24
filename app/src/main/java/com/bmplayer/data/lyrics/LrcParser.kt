package com.bmplayer.data.lyrics

import java.util.Locale

 data class LyricLine(val timeMs: Long, val text: String)

object LrcParser {
    private val timestamp = Regex("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]\\s*(.*)")

    fun parse(content: String): List<LyricLine> = content.lineSequence()
        .flatMap { line ->
            val match = timestamp.matchEntire(line.trim()) ?: return@flatMap emptySequence()
            val minutes = match.groupValues[1].toLong()
            val seconds = match.groupValues[2].toLong()
            val fraction = match.groupValues[3]
            val fractionMs = when (fraction.length) {
                1 -> fraction.toLong() * 100
                2 -> fraction.toLong() * 10
                else -> fraction.toLongOrNull() ?: 0L
            }
            sequenceOf(LyricLine(minutes * 60_000 + seconds * 1_000 + fractionMs, match.groupValues[4]))
        }
        .sortedBy(LyricLine::timeMs)
        .toList()

    fun lineAt(lines: List<LyricLine>, positionMs: Long): LyricLine? = lines.lastOrNull { it.timeMs <= positionMs }

    fun formatTimestamp(timeMs: Long): String {
        val totalSeconds = timeMs / 1_000
        return String.format(Locale.US, "[%02d:%02d.%02d]", totalSeconds / 60, totalSeconds % 60, (timeMs % 1_000) / 10)
    }
}
