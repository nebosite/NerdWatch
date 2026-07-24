package com.nerdwatch.moon

import java.time.Duration
import java.time.Instant
import kotlin.math.PI
import kotlin.math.cos

/**
 * The moon's phase from a date, by a standard synodic-month approximation
 * (accurate to well under a day — plenty for a phase graphic). Pure.
 *
 * `phaseFraction` runs 0..1 across the cycle: 0 new, 0.25 first quarter,
 * 0.5 full, 0.75 last quarter. `illumination` is the lit fraction of the disk.
 */
object MoonPhase {

    private const val SYNODIC_DAYS = 29.530588853

    /** A known new moon, 2000-01-06 18:14 UTC. */
    private val NEW_MOON_EPOCH: Instant = Instant.parse("2000-01-06T18:14:00Z")

    fun phaseFraction(now: Instant): Double {
        val days = Duration.between(NEW_MOON_EPOCH, now).toMillis() / 86_400_000.0
        val cycles = days / SYNODIC_DAYS
        var fraction = cycles - Math.floor(cycles)
        if (fraction < 0.0) fraction += 1.0
        return fraction
    }

    /** Illuminated fraction of the disk, 0 (new) .. 1 (full). */
    fun illumination(now: Instant): Double =
        (1.0 - cos(2.0 * PI * phaseFraction(now))) / 2.0

    /** True while the moon is waxing (lit on the right, N hemisphere). */
    fun isWaxing(now: Instant): Boolean = phaseFraction(now) < 0.5
}
