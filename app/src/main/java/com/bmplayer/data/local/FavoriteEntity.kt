package com.bmplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val trackId: Long,
    val addedAt: Long = System.currentTimeMillis()
)
