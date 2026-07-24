package com.nerdwatch.moon

/** Everything the moon widget draws, resolved for one instant. */
data class MoonData(
    /** 0 new … 0.25 first quarter … 0.5 full … 0.75 last quarter. */
    val phaseFraction: Double,
    /** Ring marker angle, clockwise from top (top = overhead at midnight); null if unknown. */
    val ringAngleDeg: Double?,
    /** How many clouds to draw tonight: 0, 1, or 2, by forecast cover. */
    val cloudCount: Int,
) {
    companion object {
        /** Fallback before any real data: a first-quarter moon, no marker, clear. */
        val UNKNOWN = MoonData(phaseFraction = 0.25, ringAngleDeg = null, cloudCount = 0)

        /** Cloud count by forecast cover: >60% → 2, >30% → 1, otherwise none. */
        fun cloudCountForCover(coverPercent: Double): Int = when {
            coverPercent > 60.0 -> 2
            coverPercent > 30.0 -> 1
            else -> 0
        }
    }
}
