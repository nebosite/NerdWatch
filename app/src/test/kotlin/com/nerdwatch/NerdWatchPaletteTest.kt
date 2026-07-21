package com.nerdwatch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NerdWatchPaletteTest {

    @Test
    fun `day palette is fully opaque`() {
        val alpha = (NerdWatchPalette.DAY.background ushr 24) and 0xFF
        assertEquals(0xFF, alpha)
    }

    @Test
    fun `dial stays dark enough to read at night`() {
        val background = NerdWatchPalette.DAY.background
        val red = (background shr 16) and 0xFF
        val green = (background shr 8) and 0xFF
        val blue = background and 0xFF

        assertTrue("background should be near-black", red < 32 && green < 32 && blue < 32)
    }

    @Test
    fun `accent is distinct from the background so the dial is visible`() {
        assertNotEquals(NerdWatchPalette.DAY.background, NerdWatchPalette.DAY.accent)
    }
}
