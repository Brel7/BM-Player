package com.bmplayer.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeTest {
    @Test fun startsNearEndWhilePlaying() =
        assertTrue(shouldStartCrossfade(true, false, false, 5_000, 200_000, 196_000, true))

    @Test fun neverStartsWhilePaused() =
        assertFalse(shouldStartCrossfade(false, false, false, 5_000, 200_000, 196_000, true))

    @Test fun notBeforeWindow() =
        assertFalse(shouldStartCrossfade(true, false, false, 5_000, 200_000, 100_000, true))

    @Test fun notOnLastTrack() =
        assertFalse(shouldStartCrossfade(true, false, false, 5_000, 200_000, 196_000, false))

    @Test fun skipsFirstErrorWhenNextExists() =
        assertTrue(shouldSkipAfterError(consecutiveErrors = 1, mediaItemCount = 3, hasNext = true))

    @Test fun stopsSkippingOnceEveryItemFailed() =
        assertFalse(shouldSkipAfterError(consecutiveErrors = 3, mediaItemCount = 3, hasNext = true))

    @Test fun neverSkipsWithoutNext() =
        assertFalse(shouldSkipAfterError(consecutiveErrors = 1, mediaItemCount = 3, hasNext = false))
}
