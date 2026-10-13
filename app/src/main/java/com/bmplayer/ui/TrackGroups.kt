package com.bmplayer.ui

import com.bmplayer.domain.model.Track

data class TrackGroup(val key: String, val title: String, val subtitle: String, val tracks: List<Track>)

// MediaStore reports missing tags as "<unknown>".
private fun String.isUnknownTag() = isBlank() || this == "<unknown>"

private fun List<TrackGroup>.sortedUnknownLast(unknownLabel: String) =
    sortedWith(compareBy<TrackGroup>({ it.title == unknownLabel }, { it.title.lowercase() }, { it.subtitle.lowercase() }))

fun groupByArtist(tracks: List<Track>, unknownLabel: String): List<TrackGroup> =
    tracks.groupBy { if (it.artist.isUnknownTag()) unknownLabel else it.artist }
        .map { (artist, artistTracks) ->
            TrackGroup("artist:$artist", artist, "", artistTracks.sortedWith(compareBy({ it.album.lowercase() }, { it.title.lowercase() })))
        }
        .sortedUnknownLast(unknownLabel)

fun groupByAlbum(tracks: List<Track>, unknownLabel: String): List<TrackGroup> =
    tracks.groupBy { (if (it.album.isUnknownTag()) unknownLabel else it.album) to (if (it.artist.isUnknownTag()) unknownLabel else it.artist) }
        .map { (key, albumTracks) ->
            val (album, artist) = key
            TrackGroup("album:$album\u0000$artist", album, artist, albumTracks.sortedBy { it.title.lowercase() })
        }
        .sortedUnknownLast(unknownLabel)
