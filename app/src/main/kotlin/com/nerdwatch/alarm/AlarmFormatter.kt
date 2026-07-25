package com.nerdwatch.alarm

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Formats the alarm time two ways for the picker: the absolute clock time (with
 * a weekday when it lands on a different day) and the relative offset from now.
 */
object AlarmFormatter {

    private val TIME_12 = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
    private val TIME_24 = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val WEEKDAY = DateTimeFormatter.ofPattern("EEE", Locale.US)

    /** e.g. "3:47 PM", or "WED 3:47 PM" when it falls on another day. */
    fun absolute(alarm: LocalDateTime, now: LocalDateTime, use24Hour: Boolean): String {
        val time = alarm.format(if (use24Hour) TIME_24 else TIME_12)
        return if (alarm.toLocalDate() != now.toLocalDate()) {
            "${alarm.format(WEEKDAY).uppercase(Locale.US)} $time"
        } else {
            time
        }
    }

    /** e.g. "IN 5M", "IN 2H 22M", "IN 3D 4H". */
    fun relative(offsetMinutes: Long): String {
        val m = offsetMinutes.coerceAtLeast(0)
        val days = m / 1440
        val hours = (m % 1440) / 60
        val mins = m % 60
        return when {
            days > 0 -> "IN ${days}D ${hours}H"
            hours > 0 -> "IN ${hours}H ${mins}M"
            else -> "IN ${mins}M"
        }
    }
}
