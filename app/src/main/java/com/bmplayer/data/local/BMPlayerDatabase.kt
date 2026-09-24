package com.bmplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrackEntity::class, PlaylistEntity::class, PlaylistTrackEntity::class, HistoryEntity::class, TrackMetadataOverrideEntity::class, FavoriteEntity::class],
    version = 4,
    exportSchema = true
)
abstract class BMPlayerDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun trackMetadataOverrideDao(): TrackMetadataOverrideDao
    abstract fun favoriteDao(): FavoriteDao
}
