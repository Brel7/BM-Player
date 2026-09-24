package com.bmplayer.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackMetadataOverrideDao {
    @Query("SELECT * FROM track_metadata_overrides WHERE trackId = :trackId")
    fun observe(trackId: Long): Flow<TrackMetadataOverrideEntity?>

    @Query("SELECT * FROM track_metadata_overrides")
    fun observeAll(): Flow<List<TrackMetadataOverrideEntity>>

    @Upsert
    suspend fun save(override: TrackMetadataOverrideEntity)

    @Query("DELETE FROM track_metadata_overrides WHERE trackId = :trackId")
    suspend fun clear(trackId: Long)
}
