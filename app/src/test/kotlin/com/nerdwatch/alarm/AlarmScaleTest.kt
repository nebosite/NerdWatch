package com.nerdwatch.alarm

import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmScaleTest {

    @Test
    fun `fraction endpoints map to 1 minute and 7 days`() {
        assertEquals(1.0, AlarmScale.offsetMinutes(0.0), 1e-9)
        assertEquals(10080.0, AlarmScale.offsetMinutes(1.0), 1e-6)
    }

    @Test
    fun `scale is logarithmic - halfway is the geometric mean`() {
        // Midpoint offset is sqrt(1 * 10080) ≈ 100.4 minutes.
        assertEquals(100.4, AlarmScale.offsetMinutes(0.5), 0.5)
    }

    @Test
    fun `offset and fraction are inverses`() {
        for (f in listOf(0.0, 0.2, 0.5, 0.8, 1.0)) {
            assertEquals(f, AlarmScale.fraction(AlarmScale.offsetMinutes(f)), 1e-9)
        }
    }

    @Test
    fun `fraction clamps outside the range`() {
        assertEquals(0.0, AlarmScale.fraction(0.1), 1e-9)      // under a minute
        assertEquals(1.0, AlarmScale.fraction(99999.0), 1e-9) // over a week
    }

    @Test
    fun `angles - lower-left is start, top is middle, lower-right is end`() {
        assertEquals(225.0, AlarmScale.angleForFraction(0.0), 1e-9)   // lower-left
        assertEquals(360.0, AlarmScale.angleForFraction(0.5), 1e-9)   // top (225+135)
        assertEquals(495.0, AlarmScale.angleForFraction(1.0), 1e-9)   // lower-right (≡135)
    }

    @Test
    fun `touch angle maps back to fraction`() {
        assertEquals(0.0, AlarmScale.fractionForAngle(225.0), 1e-9)   // lower-left
        assertEquals(0.5, AlarmScale.fractionForAngle(0.0), 1e-9)     // top
        assertEquals(1.0, AlarmScale.fractionForAngle(135.0), 1e-9)   // lower-right
        assertEquals(0.5, AlarmScale.fractionForAngle(360.0), 1e-9)   // top, wrapped
    }

    @Test
    fun `bottom gap snaps to the nearer end`() {
        assertEquals(1.0, AlarmScale.fractionForAngle(160.0), 1e-9)   // just past lower-right
        assertEquals(0.0, AlarmScale.fractionForAngle(200.0), 1e-9)   // just before lower-left
    }
}
