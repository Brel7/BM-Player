package com.bmplayer.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class UserPreferencesDefaultsTest {
    // Text sits directly on the photo in places; 0.45 left light text near 2.7:1 over bright photos.
    @Test fun transparentOverlayDefaultKeepsTextReadable() = assertEquals(0.6f, UserPreferences().backgroundDim, 0f)
}
