package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerTest {
    @Test fun halfway() = assertEquals(0.5f, sleepTimerProgress(30_000, 60_000), 0.0001f)

    @Test fun zeroTotalIsZero() = assertEquals(0f, sleepTimerProgress(10_000, 0), 0f)

    @Test fun clampsAboveTotal() = assertEquals(1f, sleepTimerProgress(90_000, 60_000), 0f)

    @Test fun formatsMinutes() = assertEquals("23:41", formatTimerRemaining(23 * 60_000L + 41_000))

    @Test fun formatsHours() = assertEquals("1:05:09", formatTimerRemaining(3_909_000))
}
