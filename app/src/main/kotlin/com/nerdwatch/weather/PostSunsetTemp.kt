package com.nerdwatch.weather

import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.abs

/**
 * The forecast temperature one hour after local sunset, picked from an hourly
 * series as the reading whose timestamp is closest to sunset + 1h.
 *
 * Pure (no android / no network) so it stays JVM-unit-testable — the caller
 * supplies the already-parsed hourly times, temps, and the sunset instant.
 */
object PostSunsetTemp {
    fun tempAtHourAfterSunset(
        times: List<LocalDateTime>,
        temps: List<Double>,
        sunset: LocalDateTime,
    ): Double? {
        if (times.isEmpty() || times.size != temps.size) return null
        val target = sunset.plusHours(1)
        var bestIdx = -1
        var bestDiffMin = Long.MAX_VALUE
        for (i in times.indices) {
            val diff = abs(Duration.between(times[i], target).toMinutes())
            if (diff < bestDiffMin) {
                bestDiffMin = diff
                bestIdx = i
            }
        }
        return if (bestIdx >= 0) temps[bestIdx] else null
    }
}
