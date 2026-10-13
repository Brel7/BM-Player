package com.bmplayer.ui

import com.bmplayer.domain.model.Track

/**
 * Track needs a non-null android.net.Uri, which cannot be built in JVM unit tests. Grouping and sorting never
 * read it, so the Track is allocated without its constructor and only the fields under test are set.
 */
internal fun testTrack(id: Long, title: String, artist: String = "", album: String = "", date: Long = 0, durationMs: Long = 0): Track {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    val track = unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, Track::class.java) as Track
    listOf("id" to id, "title" to title, "artist" to artist, "album" to album, "genre" to "", "dateAdded" to date, "durationMs" to durationMs)
        .forEach { (name, value) -> Track::class.java.getDeclaredField(name).apply { isAccessible = true }.set(track, value) }
    return track
}
