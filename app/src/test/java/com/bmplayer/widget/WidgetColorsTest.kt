package com.bmplayer.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetColorsTest {
    private fun contrastWithWhite(argb: Int) = 1.05 / (relativeLuminance(argb) + 0.05)

    @Test fun whiteTextContrastOnWhiteArtwork() = assertTrue(contrastWithWhite(widgetAccentBackground(0xFFFFFFFF.toInt())) >= 4.5)

    @Test fun brightOrangeGetsDarkened() = assertTrue(contrastWithWhite(widgetAccentBackground(0xFFE8A33D.toInt())) >= 4.5)

    @Test fun darkColorUnchanged() = assertEquals(0xFF101820.toInt(), widgetAccentBackground(0xFF101820.toInt()))

    @Test fun forcesOpaque() = assertEquals(0xFF, widgetAccentBackground(0x80101820.toInt()) ushr 24)
}
