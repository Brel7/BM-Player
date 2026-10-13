package com.bmplayer.ui

import com.bmplayer.domain.model.Track

fun sortTracks(tracks: List<Track>, sortOrder: String): List<Track> = when (sortOrder) {
    "date" -> tracks.sortedWith(compareByDescending<Track> { it.dateAdded }.thenBy { it.title.lowercase() })
    "random" -> tracks.shuffled()
    else -> tracks.sortedBy { it.title.lowercase() }
}

fun orderByIds(tracks: List<Track>, ids: List<Long>): List<Track> {
    val byId = tracks.associateBy { it.id }
    return ids.mapNotNull { byId[it] }
}
