package com.nerdwatch.timer

/**
 * A countdown timer as pure, immutable state.
 *
 * Like [com.nerdwatch.chrono.Chronometer], it owns no clock: everything is
 * computed against a caller-supplied monotonic `nowMs`. The timer keeps running
 * regardless of which screen is shown — it is defined solely by the moment it
 * will reach zero.
 */
data class CountdownTimer(
    /** Monotonic uptime at which the timer reaches zero, or null when unset. */
    val endAtMs: Long? = null,
) {
    fun isSet(): Boolean = endAtMs != null

    fun isRunning(nowMs: Long): Boolean = endAtMs != null && endAtMs > nowMs

    fun hasFired(nowMs: Long): Boolean = endAtMs != null && endAtMs <= nowMs

    fun remainingMs(nowMs: Long): Long =
        if (endAtMs == null) 0L else (endAtMs - nowMs).coerceAtLeast(0L)

    fun startMinutes(minutes: Int, nowMs: Long): CountdownTimer =
        copy(endAtMs = nowMs + minutes * MINUTE_MS)

    /** Adjust by whole minutes, clamped so at least [MIN_REMAINING_MS] is left. */
    fun adjustMinutes(deltaMinutes: Int, nowMs: Long): CountdownTimer {
        val end = endAtMs ?: return this
        val floor = nowMs + MIN_REMAINING_MS
        return copy(endAtMs = (end + deltaMinutes * MINUTE_MS).coerceAtLeast(floor))
    }

    fun cancel(): CountdownTimer = CountdownTimer()

    companion object {
        const val MINUTE_MS = 60_000L
        /** The design clamps adjustments so the remaining time never drops below 1s. */
        const val MIN_REMAINING_MS = 1_000L

        /** The preset minute values, in the design's grid order. */
        val PRESET_MINUTES = listOf(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
    }
}
