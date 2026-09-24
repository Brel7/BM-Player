package com.bmplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(primaryKeys = ["playlistId", "trackId"], tableName = "playlist_tracks")
data class PlaylistTrackEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int
)
