package com.bmplayer.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundImageTest {
    @Test fun landscapeFits() = assertEquals(1440 to 960, fitWithin(3000, 2000, 1440))

    @Test fun neverUpscales() = assertEquals(800 to 600, fitWithin(800, 600, 1440))

    @Test fun panoramaFits() = assertEquals(1440 to 240, fitWithin(12_000, 2_000, 1440))

    @Test fun sampleSizeForHugeImage() = assertEquals(8, sampleSizeFor(12_000, 2_000, 1440))

    @Test fun sampleSizeForSmallImage() = assertEquals(1, sampleSizeFor(800, 600, 1440))
}
