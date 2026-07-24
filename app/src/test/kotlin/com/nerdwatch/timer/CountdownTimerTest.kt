package com.nerdwatch.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CountdownTimerTest {

    @Test
    fun `unset timer is not running and has no remaining`() {
        val t = CountdownTimer()
        assertFalse(t.isSet())
        assertFalse(t.isRunning(1_000))
        assertEquals(0L, t.remainingMs(1_000))
    }

    @Test
    fun `starting sets an endpoint that counts down`() {
        val t = CountdownTimer().startMinutes(5, nowMs = 1_000)
        assertTrue(t.isRunning(1_000))
        assertEquals(5 * 60_000L, t.remainingMs(1_000))
        assertEquals(4 * 60_000L, t.remainingMs(1_000 + 60_000))
    }

    @Test
    fun `firing happens once the endpoint passes`() {
        val t = CountdownTimer().startMinutes(1, nowMs = 0)
        assertFalse(t.hasFired(59_000))
        assertTrue(t.hasFired(60_000))
        assertFalse(t.isRunning(60_000))
        assertEquals(0L, t.remainingMs(120_000))
    }

    @Test
    fun `adjusting adds or removes whole minutes`() {
        val t = CountdownTimer().startMinutes(5, nowMs = 0)
        assertEquals(6 * 60_000L, t.adjustMinutes(1, nowMs = 0).remainingMs(0))
        assertEquals(4 * 60_000L, t.adjustMinutes(-1, nowMs = 0).remainingMs(0))
    }

    @Test
    fun `adjusting down is clamped so at least one second remains`() {
        val t = CountdownTimer().startMinutes(2, nowMs = 0)
        // Removing 5 minutes from a 2-minute timer must not go negative.
        assertEquals(CountdownTimer.MIN_REMAINING_MS, t.adjustMinutes(-5, nowMs = 0).remainingMs(0))
    }

    @Test
    fun `adjusting an unset timer does nothing`() {
        val t = CountdownTimer()
        assertEquals(t, t.adjustMinutes(5, nowMs = 0))
    }

    @Test
    fun `cancel clears the timer`() {
        val t = CountdownTimer().startMinutes(10, nowMs = 0).cancel()
        assertFalse(t.isSet())
    }
}
