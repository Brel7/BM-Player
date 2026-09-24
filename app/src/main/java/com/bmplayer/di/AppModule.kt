package com.bmplayer.di

import android.content.Context
import com.bmplayer.data.mediastore.MusicScanner
import com.bmplayer.data.preferences.UserPreferencesStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideMusicScanner(@ApplicationContext context: Context) = MusicScanner(context.contentResolver)

    @Provides @Singleton
    fun providePreferences(@ApplicationContext context: Context) = UserPreferencesStore(context)
}
