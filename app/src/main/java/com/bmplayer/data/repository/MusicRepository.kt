package com.bmplayer.data.repository

import com.bmplayer.data.mediastore.MusicScanner
import com.bmplayer.data.local.TrackMetadataOverrideDao
import com.bmplayer.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val scanner: MusicScanner,
    private val metadataOverrideDao: TrackMetadataOverrideDao
) {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    suspend fun refresh() {
        val overrides = metadataOverrideDao.observeAll().first()
        _tracks.value = scanner.scan().map { track ->
            val override = overrides.firstOrNull { it.trackId == track.id } ?: return@map track
            track.copy(
                title = override.title,
                artist = override.artist,
                album = override.album,
                genre = override.genre
            )
        }
    }
}
