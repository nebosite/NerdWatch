package com.nerdwatch.solar

/** The two Kp numbers the widget shows; null while unknown. */
data class SolarData(
    val currentKp: Double?,
    val nightMaxKp: Double?,
) {
    companion object {
        val UNKNOWN = SolarData(null, null)
    }
}
