package com.nerdwatch.format

/**
 * Formats the next-event countdown shown on the face chip.
 *
 * Spec: `xH yyM` normally, `yyM` when under an hour, `NOW` at zero.
 * Pure so it can be tested without a device.
 */
object CountdownFormatter {

    fun format(totalMinutes: Long): String {
        if (totalMinutes <= 0L) return "NOW"

        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return if (hours <= 0L) "${minutes}M" else "${hours}H ${minutes}M"
    }
}
