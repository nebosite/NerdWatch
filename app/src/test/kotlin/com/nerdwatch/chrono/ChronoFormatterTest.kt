package com.nerdwatch.chrono

import org.junit.Assert.assertEquals
import org.junit.Test

class ChronoFormatterTest {

    @Test
    fun `zero formats as padded minutes and seconds`() {
        val parts = ChronoFormatter.format(0)
        assertEquals("00:00", parts.big)
        assertEquals(".00", parts.hundredths)
    }

    @Test
    fun `sub-second time shows in the hundredths`() {
        val parts = ChronoFormatter.format(230)
        assertEquals("00:00", parts.big)
        assertEquals(".23", parts.hundredths)
    }

    @Test
    fun `minutes and seconds roll over correctly`() {
        val parts = ChronoFormatter.format(65_230)
        assertEquals("01:05", parts.big)
        assertEquals(".23", parts.hundredths)
    }

    @Test
    fun `minutes past ten stay two digits`() {
        assertEquals("12:34", ChronoFormatter.format(12L * 60_000 + 34_000 + 560).big)
    }

    @Test
    fun `negative input is clamped to zero`() {
        val parts = ChronoFormatter.format(-500)
        assertEquals("00:00", parts.big)
        assertEquals(".00", parts.hundredths)
    }
}
