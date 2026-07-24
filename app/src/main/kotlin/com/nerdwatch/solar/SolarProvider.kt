package com.nerdwatch.solar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Live solar-storm data from NOAA SWPC. The planetary-K forecast product is one
 * chronological series carrying both observed and predicted 3-hourly Kp, so a
 * single fetch yields the current index and tonight's forecast peak.
 */
private const val KP_FORECAST_URL =
    "https://services.swpc.noaa.gov/products/noaa-planetary-k-index-forecast.json"

private const val REFRESH_INTERVAL_MS = 30 * 60_000L

@Composable
fun rememberSolarData(): SolarData {
    var data by remember { mutableStateOf(SolarData.UNKNOWN) }

    LaunchedEffect(Unit) {
        while (true) {
            val readings = withContext(Dispatchers.IO) { runCatching { fetchReadings() }.getOrNull() }
            if (readings != null && readings.isNotEmpty()) {
                val now = Instant.now()
                val zone = ZoneId.systemDefault()
                data = SolarData(
                    currentKp = SolarKpMath.currentKp(readings, now),
                    nightMaxKp = SolarKpMath.nightMaxKp(readings, now, zone),
                )
            }
            delay(REFRESH_INTERVAL_MS)
        }
    }

    return data
}

private fun fetchReadings(): List<KpReading> {
    val connection = (URL(KP_FORECAST_URL).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 15_000
        requestMethod = "GET"
    }
    val body = try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }
    return parseReadings(body)
}

/** Parse NOAA's array of {time_tag, kp, observed} objects into readings. */
internal fun parseReadings(json: String): List<KpReading> {
    val array = JSONArray(json)
    val readings = ArrayList<KpReading>(array.length())
    for (i in 0 until array.length()) {
        val row = array.getJSONObject(i)
        val timeTag = row.getString("time_tag")
        val instant = LocalDateTime.parse(timeTag).toInstant(ZoneOffset.UTC)
        readings.add(
            KpReading(
                time = instant,
                kp = row.getDouble("kp"),
                predicted = row.optString("observed") != "observed",
            ),
        )
    }
    return readings
}
