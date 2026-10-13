package com.bmplayer.ui

import com.bmplayer.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackOrderingTest {
    private fun t(id: Long, title: String, date: Long): Track = track("id" to id, "title" to title, "dateAdded" to date)

    @Test fun dateSortIsNewestFirst() {
        val result = sortTracks(listOf(t(1, "A", 10), t(2, "B", 30), t(3, "C", 20)), "date")
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test fun dateSortTieBreaksByTitle() {
        val result = sortTracks(listOf(t(1, "b", 10), t(2, "A", 10)), "date")
        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    @Test fun titleSortIgnoresCase() {
        val result = sortTracks(listOf(t(1, "b", 0), t(2, "A", 0)), "title")
        assertEquals(listOf(2L, 1L), result.map { it.id })
    }

    @Test fun playlistOrderFollowsIdsAndSkipsMissing() {
        val library = listOf(t(1, "Alpha", 0), t(2, "Zebra", 0))
        assertEquals(listOf(2L, 1L), orderByIds(library, listOf(2L, 99L, 1L)).map { it.id })
    }

    private companion object {
        // Track needs a non-null android.net.Uri, which cannot be built in JVM unit tests;
        // sorting never reads it, so allocate Track without its constructor and set only the sorted fields.
        fun track(vararg fields: Pair<String, Any>): Track {
            val unsafeClass = Class.forName("sun.misc.Unsafe")
            val unsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
            val track = unsafeClass.getMethod("allocateInstance", Class::class.java).invoke(unsafe, Track::class.java) as Track
            fields.forEach { (name, value) -> Track::class.java.getDeclaredField(name).apply { isAccessible = true }.set(track, value) }
            return track
        }
    }
}
