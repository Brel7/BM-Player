package com.bmplayer.data.local

import androidx.room.Entity

@Entity(primaryKeys = ["trackId", "playedAt"], tableName = "play_history")
data class HistoryEntity(
    val trackId: Long,
    val playedAt: Long = System.currentTimeMillis(),
    val completed: Boolean = false
)
