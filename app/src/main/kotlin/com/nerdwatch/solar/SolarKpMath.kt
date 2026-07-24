package com.nerdwatch.solar

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * Derives the two displayed numbers from a NOAA Kp series: the current index and
 * the peak predicted during the upcoming local night. Pure and time-injectable.
 */
object SolarKpMath {

    /** Local hours counted as night for the aurora forecast. */
    private const val NIGHT_START_HOUR = 21
    private const val NIGHT_END_HOUR = 6

    /** How far ahead to look for "tonight" — enough to span one full night. */
    private val FORECAST_WINDOW: Duration = Duration.ofHours(30)

    /** The most recent sample at or before [now]; the present 3-hour Kp. */
    fun currentKp(readings: List<KpReading>, now: Instant): Double? =
        readings.filter { !it.time.isAfter(now) }.maxByOrNull { it.time }?.kp
            ?: readings.minByOrNull { it.time }?.kp

    /** The highest Kp forecast during local night hours within the look-ahead window. */
    fun nightMaxKp(readings: List<KpReading>, now: Instant, zone: ZoneId): Double? {
        val horizon = now.plus(FORECAST_WINDOW)
        return readings
            .filter { it.time.isAfter(now) && !it.time.isAfter(horizon) }
            .filter { isNight(it.time, zone) }
            .maxByOrNull { it.kp }
            ?.kp
    }

    private fun isNight(time: Instant, zone: ZoneId): Boolean {
        val hour = time.atZone(zone).hour
        return hour >= NIGHT_START_HOUR || hour < NIGHT_END_HOUR
    }
}
