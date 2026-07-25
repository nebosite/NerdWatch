package com.nerdwatch.alarm

import kotlin.math.ln
import kotlin.math.pow

/**
 * The alarm dial's logarithmic time scale and its geometry.
 *
 * A fraction 0..1 runs along the arc from the lower-left (1 minute from now) to
 * the lower-right (7 days from now), logarithmically — so the near future gets
 * most of the dial. The arc itself sweeps 270°, from lower-left clockwise up the
 * left side, over the top and down the right, leaving a 90° gap at the bottom.
 * All angles are measured clockwise from the top.
 */
object AlarmScale {

    const val MIN_MINUTES = 1.0
    const val MAX_MINUTES = 7.0 * 24.0 * 60.0   // 7 days = 10080

    const val START_ANGLE = 225.0               // lower-left, clockwise from top
    const val SWEEP = 270.0                      // to lower-right (225 + 270 ≡ 135)

    private val ratio get() = MAX_MINUTES / MIN_MINUTES

    /** Minutes-from-now for a dial fraction (log scale). */
    fun offsetMinutes(fraction: Double): Double {
        val t = fraction.coerceIn(0.0, 1.0)
        return MIN_MINUTES * ratio.pow(t)
    }

    /** Dial fraction for a minutes-from-now offset (inverse log scale). */
    fun fraction(offsetMinutes: Double): Double {
        val o = offsetMinutes.coerceIn(MIN_MINUTES, MAX_MINUTES)
        return ln(o / MIN_MINUTES) / ln(ratio)
    }

    /** Angle (clockwise from top) of a fraction along the arc. */
    fun angleForFraction(fraction: Double): Double =
        START_ANGLE + SWEEP * fraction.coerceIn(0.0, 1.0)

    /**
     * Fraction for an angle (clockwise from top) of a touch point. Angles in the
     * bottom gap snap to the nearer end of the arc.
     */
    fun fractionForAngle(angleClockwiseFromTop: Double): Double {
        var a = angleClockwiseFromTop % 360.0
        if (a < 0.0) a += 360.0
        return when {
            a >= START_ANGLE -> (a - START_ANGLE) / SWEEP           // [225, 360)
            a <= 135.0 -> (a + 360.0 - START_ANGLE) / SWEEP         // [0, 135]
            else -> if (a < 180.0) 1.0 else 0.0                     // bottom gap → nearer end
        }.coerceIn(0.0, 1.0)
    }
}
