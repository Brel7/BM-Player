package com.bmplayer.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayNextTest {
    @Test fun insertedItemMovesRightAfterAnchor() = assertEquals(listOf(2, 0, 1, 3), moveAfter(listOf(2, 0, 3, 1), item = 3, after = 1))

    @Test fun insertedItemMovesForwardToo() = assertEquals(listOf(1, 3, 0, 2), moveAfter(listOf(1, 0, 2, 3), item = 3, after = 1))

    @Test fun pendingAdvancesWhenItsFirstTrackStarts() = assertEquals(listOf(6L), advancePending(listOf(5L, 6L), currentId = 5L))

    @Test fun pendingClearsWhenAnotherTrackStarts() = assertEquals(emptyList<Long>(), advancePending(listOf(5L, 6L), currentId = 9L))

    @Test fun pendingClearsWhenQueueEnds() = assertEquals(emptyList<Long>(), advancePending(listOf(5L), currentId = null))
}
