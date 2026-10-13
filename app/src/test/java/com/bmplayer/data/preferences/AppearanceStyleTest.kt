package com.bmplayer.data.preferences

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceStyleTest {
    @Test fun storedWins() = assertEquals("transparent", resolveAppearanceStyle("transparent", true))

    @Test fun legacyLiquidGlassMigrates() = assertEquals("liquid_glass", resolveAppearanceStyle(null, true))

    @Test fun defaultsToStandard() = assertEquals("standard", resolveAppearanceStyle(null, null))

    @Test fun unknownFallsBackToStandard() = assertEquals("standard", resolveAppearanceStyle("neon", false))
}
