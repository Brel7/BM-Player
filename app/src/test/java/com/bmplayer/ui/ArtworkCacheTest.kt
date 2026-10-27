package com.bmplayer.ui

import android.content.ComponentCallbacks2
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkCacheTest {
    @Test fun leavingTheScreenHalvesTheCache() =
        assertEquals(0.5f, artworkCacheKeepFraction(ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN), 0f)

    @Test fun aBackgroundedAppHalvesTheCache() =
        assertEquals(0.5f, artworkCacheKeepFraction(ComponentCallbacks2.TRIM_MEMORY_BACKGROUND), 0f)

    @Test fun aCriticalWarningDropsEverything() =
        assertEquals(0f, artworkCacheKeepFraction(ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL), 0f)

    @Test fun theLastWarningBeforeBeingKilledDropsEverything() =
        assertEquals(0f, artworkCacheKeepFraction(ComponentCallbacks2.TRIM_MEMORY_COMPLETE), 0f)

    @Test fun anUnknownLevelKeepsTheCache() = assertEquals(1f, artworkCacheKeepFraction(-1), 0f)
}
