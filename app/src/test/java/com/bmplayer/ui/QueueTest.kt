package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class QueueTest {
    private val library = listOf(testTrack(1, "A"), testTrack(2, "B"), testTrack(3, "C"))

    @Test fun entriesFollowQueueOrder() =
        assertEquals(listOf(0 to 3L, 1 to 1L, 2 to 2L), queueEntries(library, listOf(3L, 1L, 2L)).map { it.index to it.track.id })

    @Test fun entriesKeepDuplicates() =
        assertEquals(listOf(0 to 1L, 1 to 1L), queueEntries(library, listOf(1L, 1L)).map { it.index to it.track.id })

    @Test fun unknownIdsKeepTheirTimelineIndex() =
        assertEquals(listOf(0 to 1L, 2 to 2L), queueEntries(library, listOf(1L, 99L, 2L)).map { it.index to it.track.id })

    @Test fun emptyQueueHasNoEntries() = assertEquals(emptyList<QueueEntry>(), queueEntries(library, emptyList()))

    @Test fun moveDown() = assertEquals(listOf("b", "c", "a"), moveItem(listOf("a", "b", "c"), 0, 2))

    @Test fun moveUp() = assertEquals(listOf("c", "a", "b"), moveItem(listOf("a", "b", "c"), 2, 0))

    @Test fun moveToSamePlaceKeepsOrder() = assertEquals(listOf("a", "b", "c"), moveItem(listOf("a", "b", "c"), 1, 1))

    @Test fun dragPastHalfARowMovesOneSlot() = assertEquals(2, dragTargetIndex(1, 40f, 64, 3))

    @Test fun dragBelowHalfARowStays() = assertEquals(1, dragTargetIndex(1, 30f, 64, 3))

    @Test fun dragUpwardsMovesOneSlot() = assertEquals(0, dragTargetIndex(1, -40f, 64, 3))

    @Test fun dragPastTheEndClamps() = assertEquals(2, dragTargetIndex(1, 900f, 64, 3))

    @Test fun dragPastTheStartClamps() = assertEquals(0, dragTargetIndex(1, -900f, 64, 3))
}
