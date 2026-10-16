package com.bmplayer.ui

import com.bmplayer.domain.model.Track

fun sortTracks(tracks: List<Track>, sortOrder: String): List<Track> = when (sortOrder) {
    "date" -> tracks.sortedWith(compareByDescending<Track> { it.dateAdded }.thenBy { it.title.lowercase() })
    "random" -> tracks.shuffled()
    else -> tracks.sortedBy { it.title.lowercase() }
}

// "Damso & SZA" finds both artists; the whole query also counts, for names like "Simon & Garfunkel".
fun searchTracks(tracks: List<Track>, query: String): List<Track> {
    val terms = (query.split('&') + query).map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    if (terms.isEmpty()) return tracks
    return tracks.filter { track ->
        terms.any { term -> listOf(track.title, track.artist, track.album, track.genre).any { it.contains(term, ignoreCase = true) } }
    }
}

fun orderByIds(tracks: List<Track>, ids: List<Long>): List<Track> {
    val byId = tracks.associateBy { it.id }
    return ids.mapNotNull { byId[it] }
}
