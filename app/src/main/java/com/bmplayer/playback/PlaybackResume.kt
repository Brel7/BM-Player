package com.bmplayer.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.bmplayer.domain.model.Track

data class ResumePlan<T>(val items: List<T>, val startIndex: Int, val startPositionMs: Long)

fun <T> planResume(queueIds: List<Long>, trackId: Long?, positionMs: Long, byId: Map<Long, T>): ResumePlan<T>? {
    val ids = queueIds.filter { it in byId }
    if (ids.isEmpty()) return null
    val savedIndex = ids.indexOf(trackId)
    return ResumePlan(ids.map { byId.getValue(it) }, savedIndex.coerceAtLeast(0), if (savedIndex >= 0) positionMs else 0L)
}

fun Track.toMediaItem(): MediaItem = MediaItem.Builder()
    .setUri(uri)
    .setMediaId(id.toString())
    .setTag(this)
    .setMediaMetadata(MediaMetadata.Builder().setTitle(title).setArtist(artist).setAlbumTitle(album).setArtworkUri(artworkUri).build())
    .build()
