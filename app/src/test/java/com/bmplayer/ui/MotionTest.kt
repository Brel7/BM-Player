package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionTest {
    @Test fun scales() {
        assertEquals(1.6f, animationScale("slow"), 0f)
        assertEquals(1f, animationScale("normal"), 0f)
        assertEquals(0.6f, animationScale("fast"), 0f)
        assertEquals(0f, animationScale("off"), 0f)
    }

    @Test fun unknownIsNormal() = assertEquals(1f, animationScale("turbo"), 0f)

    @Test fun scaledRounds() = assertEquals(192, scaledDuration(320, 0.6f))

    @Test fun offIsInstant() = assertEquals(0, scaledDuration(320, 0f))

    @Test fun neverNegative() = assertEquals(0, scaledDuration(320, -1f))

    @Test fun previousRestartsAfterThreeSeconds() {
        assertFalse(previousRestartsTrack(3_000L))
        assertTrue(previousRestartsTrack(3_001L))
    }
}
