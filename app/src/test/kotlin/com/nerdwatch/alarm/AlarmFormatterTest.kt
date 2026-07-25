package com.nerdwatch.alarm

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class AlarmFormatterTest {

    private val now = LocalDateTime.of(2026, 7, 25, 14, 0)

    @Test
    fun `same-day absolute is just the time`() {
        val alarm = LocalDateTime.of(2026, 7, 25, 15, 47)
        assertEquals("3:47 PM", AlarmFormatter.absolute(alarm, now, use24Hour = false))
        assertEquals("15:47", AlarmFormatter.absolute(alarm, now, use24Hour = true))
    }

    @Test
    fun `another day gets a weekday prefix`() {
        val alarm = LocalDateTime.of(2026, 7, 27, 9, 5) // Monday
        assertEquals("MON 9:05 AM", AlarmFormatter.absolute(alarm, now, use24Hour = false))
    }

    @Test
    fun `relative minutes, hours, days`() {
        assertEquals("IN 5M", AlarmFormatter.relative(5))
        assertEquals("IN 2H 22M", AlarmFormatter.relative(142))
        assertEquals("IN 1D 1H", AlarmFormatter.relative(1500))
    }

    @Test
    fun `relative never goes negative`() {
        assertEquals("IN 0M", AlarmFormatter.relative(-10))
    }
}
