package com.nerdwatch.alarm

import java.time.Instant

/**
 * Sequential navigation over the set of active alarms by time — the neighbours
 * shown to each side of the one being edited, and the targets of a swipe.
 */
object AlarmNavigation {

    /** The latest alarm strictly before [time], or null. */
    fun previous(others: List<Instant>, time: Instant): Instant? =
        others.filter { it.isBefore(time) }.maxOrNull()

    /** The earliest alarm strictly after [time], or null. */
    fun next(others: List<Instant>, time: Instant): Instant? =
        others.filter { it.isAfter(time) }.minOrNull()
}
