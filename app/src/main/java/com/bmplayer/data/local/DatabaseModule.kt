package com.bmplayer.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BMPlayerDatabase = Room.databaseBuilder(
        context, BMPlayerDatabase::class.java, "bm_player.db"
    ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideTrackDao(database: BMPlayerDatabase): TrackDao = database.trackDao()

    @Provides
    fun providePlaylistDao(database: BMPlayerDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun provideHistoryDao(database: BMPlayerDatabase): HistoryDao = database.historyDao()

    @Provides
    fun provideTrackMetadataOverrideDao(database: BMPlayerDatabase): TrackMetadataOverrideDao = database.trackMetadataOverrideDao()

    @Provides
    fun provideFavoriteDao(database: BMPlayerDatabase): FavoriteDao = database.favoriteDao()
}
