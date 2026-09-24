package com.bmplayer.data.mediastore

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import com.bmplayer.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicScanner(private val resolver: ContentResolver) {
    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > ?"
        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(MIN_DURATION_MS.toString()),
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumId = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dateAdded = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val path = if (data >= 0) cursor.getString(data).orEmpty() else ""
                if (!isMessagingVoiceNote(path)) {
                    val mediaId = cursor.getLong(id)
                    tracks += Track(
                        id = mediaId,
                        uri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL, mediaId),
                        title = cursor.getString(title).orEmpty().ifBlank { "Piste sans titre" },
                        artist = cursor.getString(artist).orEmpty().ifBlank { "Artiste inconnu" },
                        album = cursor.getString(album).orEmpty().ifBlank { "Album inconnu" },
                        genre = "",
                        durationMs = cursor.getLong(duration),
                        artworkUri = Uri.parse("content://media/external/audio/albumart/${cursor.getLong(albumId)}"),
                        dateAdded = cursor.getLong(dateAdded)
                    )
                }
            }
        }
        tracks
    }

    private fun isMessagingVoiceNote(path: String): Boolean {
        val normalized = path.replace('\\', '/').lowercase()
        val excludedFolders = listOf(
            "whatsapp voice notes", "telegram voice", "telegram audio",
            "messenger audio", "signal/audio", "recordings/whatsapp",
            "android/media/com.whatsapp/whatsapp/media"
        )
        if (excludedFolders.any(normalized::contains)) return true
        val shortExtension = normalized.endsWith(".opus") || normalized.endsWith(".ogg")
        val messagingCache = listOf("/whatsapp/", "/telegram/", "/messenger/", "/signal/")
            .any(normalized::contains)
        return shortExtension && messagingCache
    }

    private companion object { const val MIN_DURATION_MS = 2_000L }
}
