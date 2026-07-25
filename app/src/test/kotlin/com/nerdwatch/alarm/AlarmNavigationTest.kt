package com.nerdwatch.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AlarmNavigationTest {

    private fun t(sec: Long) = Instant.ofEpochSecond(sec)

    private val alarms = listOf(t(100), t(300), t(500))

    @Test
    fun `previous is the latest before the time`() {
        assertEquals(t(300), AlarmNavigation.previous(alarms, t(400)))
        assertEquals(t(100), AlarmNavigation.previous(alarms, t(300)))
    }

    @Test
    fun `next is the earliest after the time`() {
        assertEquals(t(500), AlarmNavigation.next(alarms, t(400)))
        assertEquals(t(300), AlarmNavigation.next(alarms, t(100)))
    }

    @Test
    fun `no neighbour past the ends`() {
        assertNull(AlarmNavigation.previous(alarms, t(100)))
        assertNull(AlarmNavigation.next(alarms, t(500)))
    }

    @Test
    fun `empty set has no neighbours`() {
        assertNull(AlarmNavigation.previous(emptyList(), t(400)))
        assertNull(AlarmNavigation.next(emptyList(), t(400)))
    }
}
