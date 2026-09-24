package com.bmplayer.data.playlist

import android.net.Uri
import com.bmplayer.domain.model.Track

object M3uPlaylist {
    fun export(tracks: List<Track>): String = buildString {
        appendLine("#EXTM3U")
        tracks.forEach { track ->
            appendLine("#EXTINF:${track.durationMs / 1_000},${track.artist} - ${track.title}")
            appendLine(track.uri.toString())
        }
    }

    fun import(content: String): List<ImportedEntry> {
        val lines = content.lineSequence().map(String::trim).filter(String::isNotEmpty).toList()
        return lines.filterNot { it.startsWith("#") }.map { ImportedEntry(Uri.parse(it)) }
    }
}

data class ImportedEntry(val uri: Uri)
