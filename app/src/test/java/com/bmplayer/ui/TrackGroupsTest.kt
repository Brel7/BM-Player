package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackGroupsTest {
    @Test fun sameAlbumNameDifferentArtistsStaySeparate() {
        val groups = groupByAlbum(listOf(testTrack(1, "a", "X", "Greatest Hits"), testTrack(2, "b", "Y", "Greatest Hits")), "?")
        assertEquals(2, groups.size)
        assertEquals(listOf("X", "Y"), groups.map { it.subtitle })
    }

    @Test fun unknownArtistIsLabelledAndLast() {
        val groups = groupByArtist(listOf(testTrack(1, "a", "<unknown>"), testTrack(2, "b", ""), testTrack(3, "c", "abba")), "Artiste inconnu")
        assertEquals(listOf("abba", "Artiste inconnu"), groups.map { it.title })
        assertEquals(2, groups.last().tracks.size)
    }

    @Test fun groupsSortedIgnoringCase() =
        assertEquals(listOf("abba", "Zaz"), groupByArtist(listOf(testTrack(1, "a", "Zaz"), testTrack(2, "b", "abba")), "?").map { it.title })

    @Test fun artistTracksSortedByAlbumThenTitle() {
        val g = groupByArtist(listOf(testTrack(1, "z", "X", "B"), testTrack(2, "b", "X", "A"), testTrack(3, "a", "X", "B")), "?").single()
        assertEquals(listOf(2L, 3L, 1L), g.tracks.map { it.id })
    }

    @Test fun albumTracksSortedByTitle() =
        assertEquals(listOf(2L, 1L), groupByAlbum(listOf(testTrack(1, "b", "X", "A"), testTrack(2, "a", "X", "A")), "?").single().tracks.map { it.id })

    @Test fun trackCountUsesSingular() {
        assertEquals("1 titre", trackCount(1, english = false))
        assertEquals("0 titre", trackCount(0, english = false))
        assertEquals("2 titres", trackCount(2, english = false))
        assertEquals("1 track", trackCount(1, english = true))
        assertEquals("0 tracks", trackCount(0, english = true))
    }
}
