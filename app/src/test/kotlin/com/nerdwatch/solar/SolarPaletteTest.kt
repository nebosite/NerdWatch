package com.nerdwatch.solar

import org.junit.Assert.assertEquals
import org.junit.Test

class SolarPaletteTest {

    private fun a(argb: Int) = (argb ushr 24) and 0xFF
    private fun r(argb: Int) = (argb shr 16) and 0xFF
    private fun g(argb: Int) = (argb shr 8) and 0xFF
    private fun b(argb: Int) = argb and 0xFF

    @Test
    fun `below three is fully transparent`() {
        assertEquals(0, a(SolarPalette.backgroundArgb(2.0)))
        assertEquals(0, a(SolarPalette.backgroundArgb(0.0)))
        assertEquals(0, a(SolarPalette.backgroundArgb(3.0)))
    }

    @Test
    fun `four is opaque green`() {
        val c = SolarPalette.backgroundArgb(4.0)
        assertEquals(0xFF, a(c))
        assertEquals(0x2E, r(c))
        assertEquals(0xCC, g(c))
        assertEquals(0x40, b(c))
    }

    @Test
    fun `nine and above is magenta`() {
        val c = SolarPalette.backgroundArgb(9.0)
        assertEquals(0xFF, a(c))
        assertEquals(0xE0, r(c))
        assertEquals(0x40, g(c))
        assertEquals(0xFB, b(c))
        assertEquals(c, SolarPalette.backgroundArgb(11.0))
    }

    @Test
    fun `three point five is half-faded green`() {
        // Green rgb, alpha halfway from 0 to 255.
        val c = SolarPalette.backgroundArgb(3.5)
        assertEquals(127, a(c))
        assertEquals(0x2E, r(c))
        assertEquals(0xCC, g(c))
    }

    @Test
    fun `six is halfway between yellow and orange`() {
        val c = SolarPalette.backgroundArgb(6.0)
        assertEquals(0xFF, a(c))
        // yellow #FFDC00 -> orange #FF851B, midpoint per channel
        assertEquals((0xFF + 0xFF) / 2, r(c))
        assertEquals((0xDC + 0x85) / 2, g(c))
        assertEquals((0x00 + 0x1B) / 2, b(c))
    }
}
