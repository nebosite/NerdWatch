package com.nerdwatch.moon

/** Everything the moon widget draws, resolved for one instant. */
data class MoonData(
    /** 0 new … 0.25 first quarter … 0.5 full … 0.75 last quarter. */
    val phaseFraction: Double,
    /** Ring marker angle, clockwise from top (top = overhead at midnight); null if unknown. */
    val ringAngleDeg: Double?,
    /** True when tonight is forecast at ≥50% cloud cover. */
    val cloudy: Boolean,
) {
    companion object {
        /** Fallback before any real data: a first-quarter moon, no marker, clear. */
        val UNKNOWN = MoonData(phaseFraction = 0.25, ringAngleDeg = null, cloudy = false)
    }
}
