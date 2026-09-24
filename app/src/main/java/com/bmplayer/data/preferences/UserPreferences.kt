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
    val accentLight: String = "#B9750F",
    val accentDark: String = "#E8A33D",
    val excludedFolders: Set<String> = emptySet(),
    val crossfadeSeconds: Int = 5,
    val artworkShape: String = "rounded",
    val language: String = "fr"
)

class UserPreferencesStore(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.dataStore.data.map { values ->
        UserPreferences(
            darkTheme = values[Keys.DARK_THEME] ?: false,
            accentLight = values[Keys.ACCENT_LIGHT] ?: "#B9750F",
            accentDark = values[Keys.ACCENT_DARK] ?: "#E8A33D",
            excludedFolders = values[Keys.EXCLUDED_FOLDERS]?.split("|")?.filter(String::isNotBlank)?.toSet() ?: emptySet(),
            crossfadeSeconds = values[Keys.CROSSFADE_SECONDS] ?: 5,
            artworkShape = values[Keys.ARTWORK_SHAPE] ?: "rounded",
            language = values[Keys.LANGUAGE] ?: "fr"
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) = context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    suspend fun setAccent(light: String, dark: String) = context.dataStore.edit {
        it[Keys.ACCENT_LIGHT] = light
        it[Keys.ACCENT_DARK] = dark
    }
    suspend fun resetTheme() = context.dataStore.edit {
        it.remove(Keys.ACCENT_LIGHT)
        it.remove(Keys.ACCENT_DARK)
    }
    suspend fun setExcludedFolders(folders: Set<String>) = context.dataStore.edit {
        it[Keys.EXCLUDED_FOLDERS] = folders.joinToString("|")
    }
    suspend fun setCrossfadeSeconds(seconds: Int) = context.dataStore.edit { it[Keys.CROSSFADE_SECONDS] = seconds.coerceIn(0, 15) }
    suspend fun setArtworkShape(shape: String) = context.dataStore.edit { it[Keys.ARTWORK_SHAPE] = shape }
    suspend fun setLanguage(language: String) = context.dataStore.edit { it[Keys.LANGUAGE] = language }

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val ACCENT_LIGHT = stringPreferencesKey("accent_light")
        val ACCENT_DARK = stringPreferencesKey("accent_dark")
        val EXCLUDED_FOLDERS = stringPreferencesKey("excluded_folders")
        val CROSSFADE_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("crossfade_seconds")
        val ARTWORK_SHAPE = stringPreferencesKey("artwork_shape")
        val LANGUAGE = stringPreferencesKey("language")
    }
}
