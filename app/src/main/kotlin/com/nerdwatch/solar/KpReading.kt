package com.nerdwatch.solar

import java.time.Instant

/** One 3-hourly NOAA planetary-K sample. */
data class KpReading(
    val time: Instant,
    val kp: Double,
    val predicted: Boolean,
)
