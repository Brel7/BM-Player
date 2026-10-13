package com.bmplayer.ui

import com.bmplayer.domain.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackOrderingTest {
    private fun t(id: Long, title: String, date: Long): Track = testTrack(id, title, date = date)

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

}
