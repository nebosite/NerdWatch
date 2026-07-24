package com.nerdwatch.moon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MoonPositionTest {

    @Test
    fun `transit maps to the top of the ring`() {
        assertEquals(0.0, MoonPosition.ringAngleFromHourAngle(0.0), 0.001)
    }

    @Test
    fun `rising in the east maps to the right`() {
        // 6 hours before transit = hour angle -90 degrees = rising.
        assertEquals(90.0, MoonPosition.ringAngleFromHourAngle(-90.0), 0.001)
    }

    @Test
    fun `setting in the west maps to the left`() {
        assertEquals(270.0, MoonPosition.ringAngleFromHourAngle(90.0), 0.001)
    }

    @Test
    fun `nadir maps to the bottom`() {
        assertEquals(180.0, MoonPosition.ringAngleFromHourAngle(180.0), 0.001)
        assertEquals(180.0, MoonPosition.ringAngleFromHourAngle(-180.0), 0.001)
    }

    @Test
    fun `right ascension is a valid angle`() {
        val ra = MoonPosition.rightAscensionDeg(Instant.parse("2026-07-24T00:00:00Z"))
        assertTrue("RA $ra out of range", ra in 0.0..360.0)
    }

    @Test
    fun `ring angle is always in range`() {
        val phi = MoonPosition.ringAngleDegrees(Instant.parse("2026-07-24T07:00:00Z"), -122.0)
        assertTrue("phi $phi out of range", phi in 0.0..360.0)
    }
}
