package com.nerdwatch.moon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MoonPhaseTest {

    @Test
    fun `known new moon reads near zero illumination`() {
        // 2024-07-05 22:57 UTC was a new moon.
        val illum = MoonPhase.illumination(Instant.parse("2024-07-05T22:57:00Z"))
        assertTrue("expected near-dark, was $illum", illum < 0.02)
    }

    @Test
    fun `known full moon reads near full illumination`() {
        // 2024-07-21 10:17 UTC was a full moon.
        val illum = MoonPhase.illumination(Instant.parse("2024-07-21T10:17:00Z"))
        assertTrue("expected near-full, was $illum", illum > 0.98)
    }

    @Test
    fun `phase fraction stays within the cycle`() {
        val f = MoonPhase.phaseFraction(Instant.parse("2026-07-24T00:00:00Z"))
        assertTrue(f in 0.0..1.0)
    }

    @Test
    fun `waxing before full, waning after`() {
        // A few days after new (waxing crescent) vs a few days after full (waning).
        assertTrue(MoonPhase.isWaxing(Instant.parse("2024-07-09T00:00:00Z")))
        assertFalse(MoonPhase.isWaxing(Instant.parse("2024-07-25T00:00:00Z")))
    }

    @Test
    fun `first quarter is about half lit`() {
        // 2024-07-13 was near first quarter.
        val illum = MoonPhase.illumination(Instant.parse("2024-07-13T22:49:00Z"))
        assertEquals(0.5, illum, 0.1)
    }
}
