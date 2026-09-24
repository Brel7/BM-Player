package com.bmplayer.domain.model

import android.net.Uri

 data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val durationMs: Long,
    val artworkUri: Uri? = null,
    val dateAdded: Long = 0L
)
