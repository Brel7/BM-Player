package com.bmplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "track_metadata_overrides")
data class TrackMetadataOverrideEntity(
    @PrimaryKey val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val year: String
)
