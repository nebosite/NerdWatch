package com.nerdwatch.timer

import java.util.Locale

/**
 * Formats a remaining duration two ways: `M:SS` for the big running display and
 * `H:MM:SS` for the small readout under the TIMER button.
 *
 * Both round up to the next whole second so a timer never shows `0:00` while
 * time still remains.
 */
object TimerFormatter {

    /** `M:SS`, minutes not zero-padded (design's big running readout). */
    fun big(remainingMs: Long): String {
        val totalSeconds = ceilSeconds(remainingMs)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    /** `H:MM:SS` (design's under-button readout). */
    fun compact(remainingMs: Long): String {
        val totalSeconds = ceilSeconds(remainingMs)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    }

    private fun ceilSeconds(remainingMs: Long): Long {
        val ms = remainingMs.coerceAtLeast(0L)
        return (ms + 999L) / 1000L
    }
}
