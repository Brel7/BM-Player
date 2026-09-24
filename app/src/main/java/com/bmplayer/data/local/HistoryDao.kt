package com.bmplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Insert
    suspend fun record(item: HistoryEntity)

    @Query("SELECT * FROM play_history ORDER BY playedAt DESC LIMIT 100")
    fun observeRecent(): Flow<List<HistoryEntity>>

    @Query("SELECT trackId, COUNT(*) AS playCount FROM play_history GROUP BY trackId ORDER BY playCount DESC LIMIT 50")
    fun observeMostPlayed(): Flow<List<MostPlayedTrack>>
}

data class MostPlayedTrack(val trackId: Long, val playCount: Int)
