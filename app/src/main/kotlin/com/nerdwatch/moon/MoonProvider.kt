package com.nerdwatch.moon

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.nerdwatch.location.currentLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Assembles [MoonData]: the phase (from the date, always available), and — when
 * a location is known — the sky-position marker for tonight's midnight and
 * whether tonight is forecast ≥50% cloudy. Without location, the marker and
 * cloud are simply absent.
 */
private const val REFRESH_INTERVAL_MS = 30 * 60_000L

@Composable
fun rememberMoonData(): MoonData {
    val context = LocalContext.current
    var data by remember { mutableStateOf(MoonData.UNKNOWN) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = Instant.now()
            val zone = ZoneId.systemDefault()
            val phase = MoonPhase.phaseFraction(now)
            val midnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant()

            val location = currentLocation(context)
            val ringAngle = location?.let { MoonPosition.ringAngleDegrees(midnight, it.longitude) }
            val cloudCount = location?.let {
                withContext(Dispatchers.IO) {
                    runCatching { cloudCountTonight(it.latitude, it.longitude, zone) }.getOrDefault(0)
                }
            } ?: 0

            data = MoonData(phaseFraction = phase, ringAngleDeg = ringAngle, cloudCount = cloudCount)
            delay(REFRESH_INTERVAL_MS)
        }
    }
    return data
}

/** Cloud count from the average cover across tonight's local night hours. */
private fun cloudCountTonight(lat: Double, lon: Double, zone: ZoneId): Int {
    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
        "&hourly=cloud_cover&forecast_days=2&timezone=auto"
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 15_000
    }
    val body = try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }

    val hourly = JSONObject(body).getJSONObject("hourly")
    val times = hourly.getJSONArray("time")
    val cover = hourly.getJSONArray("cloud_cover")

    val now = LocalDateTime.now(zone)
    val horizon = now.plusHours(18)
    var sum = 0.0
    var count = 0
    for (i in 0 until times.length()) {
        val t = LocalDateTime.parse(times.getString(i))
        if (t.isBefore(now) || t.isAfter(horizon)) continue
        if (t.hour >= 21 || t.hour < 6) {
            sum += cover.getDouble(i)
            count++
        }
    }
    return if (count == 0) 0 else MoonData.cloudCountForCover(sum / count)
}
