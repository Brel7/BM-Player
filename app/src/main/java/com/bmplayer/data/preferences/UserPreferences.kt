package com.bmplayer.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("bm_player_preferences")

data class UserPreferences(
    val darkTheme: Boolean = false,
    val excludedFolders: Set<String> = emptySet(),
    val crossfadeSeconds: Int = 5,
    val artworkShape: String = "rounded",
    val language: String = "fr",
    val playbackQueueIds: List<Long> = emptyList(),
    val playbackTrackId: Long? = null,
    val playbackPositionMs: Long = 0L,
    val playbackWasPlaying: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val sortOrder: String = "title",
    val liquidGlass: Boolean = false,
    val effectsEnabled: Boolean = false,
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0
)

class UserPreferencesStore(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            darkTheme = values[Keys.DARK_THEME] ?: false,
            excludedFolders = values[Keys.EXCLUDED_FOLDERS]?.split("|")?.filter(String::isNotBlank)?.toSet() ?: emptySet(),
            crossfadeSeconds = values[Keys.CROSSFADE_SECONDS] ?: 5,
            artworkShape = values[Keys.ARTWORK_SHAPE] ?: "rounded",
            language = values[Keys.LANGUAGE] ?: "fr",
            playbackQueueIds = values[Keys.PLAYBACK_QUEUE]?.split(",")?.mapNotNull(String::toLongOrNull) ?: emptyList(),
            playbackTrackId = values[Keys.PLAYBACK_TRACK]?.toLongOrNull(),
            playbackPositionMs = values[Keys.PLAYBACK_POSITION] ?: 0L,
            playbackWasPlaying = values[Keys.PLAYBACK_WAS_PLAYING] ?: false,
            shuffleEnabled = values[Keys.SHUFFLE_ENABLED] ?: false,
            repeatMode = values[Keys.REPEAT_MODE] ?: 0,
            sortOrder = values[Keys.SORT_ORDER] ?: "title",
            liquidGlass = values[Keys.LIQUID_GLASS] ?: false,
            effectsEnabled = values[Keys.EFFECTS_ENABLED] ?: false,
            bassStrength = values[Keys.BASS_STRENGTH] ?: 0,
            virtualizerStrength = values[Keys.VIRTUALIZER_STRENGTH] ?: 0
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    suspend fun setExcludedFolders(folders: Set<String>) = context.dataStore.edit {
        it[Keys.EXCLUDED_FOLDERS] = folders.joinToString("|")
    }
    suspend fun setCrossfadeSeconds(seconds: Int) = context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 15) }
    suspend fun setArtworkShape(shape: String) = context.dataStore.edit { it[Keys.ARTWORK_SHAPE] = shape }
    suspend fun setLanguage(language: String) = context.dataStore.edit { it[Keys.LANGUAGE] = language }
    suspend fun setSortOrder(sortOrder: String) = context.dataStore.edit { it[Keys.SORT_ORDER] = sortOrder }
    suspend fun setLiquidGlass(enabled: Boolean) = context.dataStore.edit { it[Keys.LIQUID_GLASS] = enabled }
    suspend fun setEffectsEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.EFFECTS_ENABLED] = enabled }
    suspend fun setBassStrength(strength: Int) = context.dataStore.edit { it[Keys.BASS_STRENGTH] = strength.coerceIn(0, 1000) }
    suspend fun setVirtualizerStrength(strength: Int) = context.dataStore.edit { it[Keys.VIRTUALIZER_STRENGTH] = strength.coerceIn(0, 1000) }
    suspend fun savePlaybackState(
        queueIds: List<Long>,
        trackId: Long?,
        positionMs: Long,
        wasPlaying: Boolean,
        shuffleEnabled: Boolean,
        repeatMode: Int
    ) = context.dataStore.edit { values ->
        values[Keys.PLAYBACK_QUEUE] = queueIds.joinToString(",")
        if (trackId == null) values.remove(Keys.PLAYBACK_TRACK) else values[Keys.PLAYBACK_TRACK] = trackId.toString()
        values[Keys.PLAYBACK_POSITION] = positionMs.coerceAtLeast(0L)
        values[Keys.PLAYBACK_WAS_PLAYING] = wasPlaying
        values[Keys.SHUFFLE_ENABLED] = shuffleEnabled
        values[Keys.REPEAT_MODE] = repeatMode
    }

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val EXCLUDED_FOLDERS = stringPreferencesKey("excluded_folders")
        val CROSSFADE_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("crossfade_seconds")
        val ARTWORK_SHAPE = stringPreferencesKey("artwork_shape")
        val LANGUAGE = stringPreferencesKey("language")
        val PLAYBACK_QUEUE = stringPreferencesKey("playback_queue_ids")
        val PLAYBACK_TRACK = stringPreferencesKey("playback_track_id")
        val PLAYBACK_POSITION = androidx.datastore.preferences.core.longPreferencesKey("playback_position_ms")
        val PLAYBACK_WAS_PLAYING = booleanPreferencesKey("playback_was_playing")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val REPEAT_MODE = androidx.datastore.preferences.core.intPreferencesKey("repeat_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val LIQUID_GLASS = booleanPreferencesKey("liquid_glass")
        val EFFECTS_ENABLED = booleanPreferencesKey("effects_enabled")
        val BASS_STRENGTH = androidx.datastore.preferences.core.intPreferencesKey("bass_strength")
        val VIRTUALIZER_STRENGTH = androidx.datastore.preferences.core.intPreferencesKey("virtualizer_strength")
    }
}
