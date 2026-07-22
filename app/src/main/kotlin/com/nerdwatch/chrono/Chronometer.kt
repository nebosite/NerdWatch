package com.nerdwatch.chrono

/**
 * A stopwatch as pure, immutable state — no Android, no clock of its own.
 *
 * Elapsed time is always computed against a caller-supplied `nowMs` (a
 * monotonic timestamp such as `SystemClock.uptimeMillis`), which keeps this
 * class free of side effects and fully unit-testable.
 */
data class Chronometer(
    /** Time banked from previous runs. */
    val accumulatedMs: Long = 0L,
    /** Monotonic timestamp the current run began, or null when stopped. */
    val runningSinceMs: Long? = null,
) {
    val isRunning: Boolean get() = runningSinceMs != null

    /** True once tapped, until reset — this is what repurposes the time display. */
    val isEngaged: Boolean get() = isRunning || accumulatedMs > 0L

    fun elapsedMs(nowMs: Long): Long {
        val live = runningSinceMs?.let { (nowMs - it).coerceAtLeast(0L) } ?: 0L
        return accumulatedMs + live
    }

    /** Tap: start if stopped, or bank the run and stop if running. Accumulates. */
    fun toggle(nowMs: Long): Chronometer =
        if (runningSinceMs != null) {
            copy(accumulatedMs = elapsedMs(nowMs), runningSinceMs = null)
        } else {
            copy(runningSinceMs = nowMs)
        }

    /** Long-press: back to zero and stopped, returning the display to the clock. */
    fun reset(): Chronometer = Chronometer()
}
