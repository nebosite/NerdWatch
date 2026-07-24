package com.nerdwatch.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerFormatterTest {

    @Test
    fun `big shows minutes and padded seconds`() {
        assertEquals("5:00", TimerFormatter.big(5 * 60_000L))
        assertEquals("0:09", TimerFormatter.big(9_000L))
        assertEquals("12:34", TimerFormatter.big(12 * 60_000L + 34_000L))
    }

    @Test
    fun `remaining rounds up so a running timer never reads zero early`() {
        // 4.2s left should read 0:05, not 0:04.
        assertEquals("0:05", TimerFormatter.big(4_200L))
        assertEquals("0:00", TimerFormatter.big(0L))
    }

    @Test
    fun `compact shows hours minutes seconds`() {
        assertEquals("0:05:00", TimerFormatter.compact(5 * 60_000L))
        assertEquals("1:02:03", TimerFormatter.compact(3_600_000L + 2 * 60_000L + 3_000L))
    }
}
