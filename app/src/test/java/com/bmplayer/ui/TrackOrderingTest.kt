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

    private val library = listOf(testTrack(1, "Macarena", "Damso"), testTrack(2, "Kill Bill", "SZA"), testTrack(3, "Other", "Someone"), testTrack(4, "Boxer", "Simon & Garfunkel"))

    @Test fun ampersandSearchesEachArtist() = assertEquals(listOf(1L, 2L), searchTracks(library, "Damso & SZA").map { it.id })

    @Test fun ampersandInsideANameStillMatches() = assertEquals(listOf(4L), searchTracks(library, "simon & garfunkel").map { it.id })

    @Test fun trailingAmpersandWhileTyping() = assertEquals(listOf(1L), searchTracks(library, "damso &").map { it.id })

    @Test fun blankSearchKeepsAll() = assertEquals(4, searchTracks(library, "  ").size)

}
