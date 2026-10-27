package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    @Test fun settingsGoesToPlayingWhenTrack() = assertEquals(1, backDestination(2, hasTrack = true))

    @Test fun settingsGoesToLibraryWithoutTrack() = assertEquals(0, backDestination(2, hasTrack = false))

    @Test fun favoritesAndPlaylistsFollowSameRule() {
        assertEquals(1, backDestination(3, true))
        assertEquals(0, backDestination(4, false))
    }

    @Test fun playingGoesToLibrary() = assertEquals(0, backDestination(1, hasTrack = true))

    @Test fun libraryExits() = assertNull(backDestination(0, hasTrack = true))

    @Test fun forwardFollowsTabOrder() {
        assertTrue(isForwardTransition(0, 1))
        assertTrue(isForwardTransition(4, 2))
        assertFalse(isForwardTransition(3, 1))
    }

    @Test fun dragDownCollapsesTheBar() = assertEquals(true, barCollapseIntent(60f, 48f))

    @Test fun dragUpReopensTheBar() = assertEquals(false, barCollapseIntent(-60f, 48f))

    @Test fun shortDragChangesNothing() = assertNull(barCollapseIntent(20f, 48f))
}
