package com.bmplayer.ui

import com.bmplayer.domain.model.Track
import kotlin.math.roundToInt

/**
 * [index] is the track's place in the Media3 timeline, which a move renumbers; [uid] stays with the track
 * across a move, which is what the list needs as a row key for the right row to animate.
 */
data class QueueEntry(val index: Int, val track: Track, val uid: String)

fun queueEntries(tracks: List<Track>, ids: List<Long>): List<QueueEntry> {
    val byId = tracks.associateBy { it.id }
    val seen = mutableMapOf<Long, Int>()
    return ids.mapIndexedNotNull { index, id ->
        byId[id]?.let { track ->
            val occurrence = seen.getOrDefault(id, 0)
            seen[id] = occurrence + 1
            QueueEntry(index, track, "$id#$occurrence")
        }
    }
}

fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
    if (from == to || from !in list.indices || to !in list.indices) return list
    val moved = list.toMutableList()
    moved.add(to, moved.removeAt(from))
    return moved
}

/** The dragged row changes place once it has travelled more than half a row. */
fun dragTargetIndex(from: Int, offsetPx: Float, rowHeightPx: Int, size: Int): Int =
    (from + (offsetPx / rowHeightPx.coerceAtLeast(1)).roundToInt()).coerceIn(0, (size - 1).coerceAtLeast(0))
