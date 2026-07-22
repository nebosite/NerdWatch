package com.nerdwatch.chrono

import java.util.Locale

/**
 * Splits a stopwatch elapsed time into the two pieces the face shows: `MM:SS`
 * big and `.hh` hundredths in the seconds box.
 */
object ChronoFormatter {

    data class Parts(
        /** `MM:SS`, minutes zero-padded, for the main numerals. */
        val big: String,
        /** `.hh` hundredths, for the bordered seconds box. */
        val hundredths: String,
    )

    fun format(elapsedMs: Long): Parts {
        val ms = elapsedMs.coerceAtLeast(0L)
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val hundredths = (ms / 10) % 100

        return Parts(
            big = String.format(Locale.US, "%02d:%02d", minutes, seconds),
            hundredths = String.format(Locale.US, ".%02d", hundredths),
        )
    }
}
