package com.nerdwatch.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class PostSunsetTempTest {

    private fun hoursFrom(start: LocalDateTime, temps: List<Double>): Pair<List<LocalDateTime>, List<Double>> {
        val times = temps.indices.map { start.plusHours(it.toLong()) }
        return times to temps
    }

    @Test
    fun `picks the hourly reading nearest sunset plus one hour`() {
        val start = LocalDateTime.of(2026, 7, 27, 18, 0)
        // 18:00=70, 19:00=68, 20:00=66, 21:00=64, 22:00=62, 23:00=60
        val (times, temps) = hoursFrom(start, listOf(70.0, 68.0, 66.0, 64.0, 62.0, 60.0))
        // Sunset 20:47 → +1h = 21:47 → nearest sample is 22:00 (62).
        val sunset = LocalDateTime.of(2026, 7, 27, 20, 47)
        assertEquals(62.0, PostSunsetTemp.tempAtHourAfterSunset(times, temps, sunset)!!, 0.0001)
    }

    @Test
    fun `sunset on the hour lands exactly one hour later`() {
        val start = LocalDateTime.of(2026, 7, 27, 18, 0)
        val (times, temps) = hoursFrom(start, listOf(70.0, 68.0, 66.0, 64.0, 62.0))
        // Sunset 20:00 → +1h = 21:00 → exactly the 21:00 sample (64).
        val sunset = LocalDateTime.of(2026, 7, 27, 20, 0)
        assertEquals(64.0, PostSunsetTemp.tempAtHourAfterSunset(times, temps, sunset)!!, 0.0001)
    }

    @Test
    fun `empty or mismatched series yields null`() {
        val sunset = LocalDateTime.of(2026, 7, 27, 20, 0)
        assertNull(PostSunsetTemp.tempAtHourAfterSunset(emptyList(), emptyList(), sunset))
        assertNull(
            PostSunsetTemp.tempAtHourAfterSunset(
                listOf(sunset), listOf(1.0, 2.0), sunset,
            ),
        )
    }
}
