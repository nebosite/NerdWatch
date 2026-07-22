package com.nerdwatch.chrono

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChronometerTest {

    @Test
    fun `fresh chronometer is stopped, zeroed and not engaged`() {
        val c = Chronometer()
        assertFalse(c.isRunning)
        assertFalse(c.isEngaged)
        assertEquals(0L, c.elapsedMs(1_000))
    }

    @Test
    fun `running elapsed counts from the start timestamp`() {
        val c = Chronometer().toggle(nowMs = 1_000)
        assertTrue(c.isRunning)
        assertTrue(c.isEngaged)
        assertEquals(2_500L, c.elapsedMs(3_500))
    }

    @Test
    fun `stopping banks the elapsed time and freezes it`() {
        val stopped = Chronometer()
            .toggle(nowMs = 1_000)
            .toggle(nowMs = 6_000)

        assertFalse(stopped.isRunning)
        assertTrue(stopped.isEngaged)
        // Frozen: later 'now' values do not advance a stopped chronometer.
        assertEquals(5_000L, stopped.elapsedMs(9_999))
    }

    @Test
    fun `restarting accumulates on top of banked time`() {
        val resumed = Chronometer()
            .toggle(nowMs = 1_000)   // start
            .toggle(nowMs = 6_000)   // stop at 5s
            .toggle(nowMs = 10_000)  // resume

        assertEquals(7_000L, resumed.elapsedMs(12_000))
    }

    @Test
    fun `reset returns to a fresh chronometer`() {
        val reset = Chronometer()
            .toggle(nowMs = 1_000)
            .toggle(nowMs = 6_000)
            .reset()

        assertEquals(Chronometer(), reset)
        assertFalse(reset.isEngaged)
    }

    @Test
    fun `elapsed never goes negative if now precedes the start`() {
        val c = Chronometer().toggle(nowMs = 5_000)
        assertEquals(0L, c.elapsedMs(4_000))
    }
}
