package com.nerdwatch.weather

/**
 * The two temperatures the TEMP widget shows: the current reading and the
 * forecast for one hour after tonight's local sunset. Either is null while
 * unknown (no location, no network).
 */
data class WeatherData(
    val currentTempF: Int?,
    val postSunsetTempF: Int?,
) {
    companion object {
        val UNKNOWN = WeatherData(null, null)
    }
}
