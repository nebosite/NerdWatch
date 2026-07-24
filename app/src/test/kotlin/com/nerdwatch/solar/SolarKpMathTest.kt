package com.nerdwatch.solar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class SolarKpMathTest {

    private val utc = ZoneOffset.UTC

    private fun at(iso: String) = Instant.parse(iso)

    private fun reading(iso: String, kp: Double, predicted: Boolean = false) =
        KpReading(at(iso), kp, predicted)

    @Test
    fun `current kp is the latest sample at or before now`() {
        val readings = listOf(
            reading("2026-07-24T00:00:00Z", 2.0),
            reading("2026-07-24T03:00:00Z", 3.0),
            reading("2026-07-24T06:00:00Z", 4.0, predicted = true),
        )
        val now = at("2026-07-24T04:30:00Z")
        assertEquals(3.0, SolarKpMath.currentKp(readings, now)!!, 0.0001)
    }

    @Test
    fun `night max picks the peak during local night hours only`() {
        // UTC zone: night is hour >= 21 or < 6.
        val readings = listOf(
            reading("2026-07-24T12:00:00Z", 8.0, predicted = true), // daytime, ignored
            reading("2026-07-24T22:00:00Z", 5.0, predicted = true), // night
            reading("2026-07-25T03:00:00Z", 6.0, predicted = true), // night, peak
            reading("2026-07-25T15:00:00Z", 9.0, predicted = true), // daytime, ignored
        )
        val now = at("2026-07-24T10:00:00Z")
        assertEquals(6.0, SolarKpMath.nightMaxKp(readings, now, utc)!!, 0.0001)
    }

    @Test
    fun `night max ignores samples beyond the look-ahead window`() {
        val readings = listOf(
            reading("2026-07-26T03:00:00Z", 7.0, predicted = true), // >30h away
        )
        val now = at("2026-07-24T10:00:00Z")
        assertNull(SolarKpMath.nightMaxKp(readings, now, utc))
    }

    @Test
    fun `night max ignores past samples`() {
        val readings = listOf(
            reading("2026-07-24T02:00:00Z", 7.0), // night but in the past
        )
        val now = at("2026-07-24T10:00:00Z")
        assertNull(SolarKpMath.nightMaxKp(readings, now, utc))
    }

    @Test
    fun `local zone shifts which samples count as night`() {
        // 08:00 UTC is night (02:00) in UTC-6.
        val readings = listOf(reading("2026-07-25T08:00:00Z", 5.0, predicted = true))
        val now = at("2026-07-24T20:00:00Z")
        val minus6 = ZoneId.of("America/Chicago")
        assertEquals(5.0, SolarKpMath.nightMaxKp(readings, now, minus6)!!, 0.0001)
    }
}
