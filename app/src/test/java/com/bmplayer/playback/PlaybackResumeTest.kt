package com.bmplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackResumeTest {
    private val library = mapOf(1L to "un", 2L to "deux", 3L to "trois")

    @Test fun keepsOrderAndStartsAtSavedTrack() {
        val plan = planResume(listOf(3L, 1L, 2L), trackId = 1L, positionMs = 5_000L, byId = library)!!
        assertEquals(listOf("trois", "un", "deux"), plan.items)
        assertEquals(1, plan.startIndex)
        assertEquals(5_000L, plan.startPositionMs)
    }

    @Test fun skipsMissingTracks() {
        val plan = planResume(listOf(3L, 9L, 1L), trackId = 1L, positionMs = 0L, byId = library)!!
        assertEquals(listOf("trois", "un"), plan.items)
        assertEquals(1, plan.startIndex)
    }

    @Test fun savedTrackMissingStartsAtBeginning() {
        val plan = planResume(listOf(3L, 1L), trackId = 9L, positionMs = 42_000L, byId = library)!!
        assertEquals(0, plan.startIndex)
        assertEquals(0L, plan.startPositionMs)
    }

    @Test fun nothingToResume() = assertNull(planResume(listOf(9L), trackId = 9L, positionMs = 0L, byId = library))
}
