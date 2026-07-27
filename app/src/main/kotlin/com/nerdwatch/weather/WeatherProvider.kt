package com.nerdwatch.weather

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
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Live temperatures from Open-Meteo: the current reading and the forecast for one
 * hour after tonight's local sunset. Refreshes on the shared half-hour cadence;
 * without a location the widget simply shows nothing new (the stub current temp
 * stays and the forecast is absent).
 */
private const val REFRESH_INTERVAL_MS = 30 * 60_000L

@Composable
fun rememberWeatherData(): WeatherData {
    val context = LocalContext.current
    var data by remember { mutableStateOf(WeatherData.UNKNOWN) }

    LaunchedEffect(Unit) {
        while (true) {
            val zone = ZoneId.systemDefault()
            val location = currentLocation(context)
            if (location != null) {
                val fetched = withContext(Dispatchers.IO) {
                    runCatching { fetchWeather(location.latitude, location.longitude, zone) }.getOrNull()
                }
                if (fetched != null) data = fetched
            }
            delay(REFRESH_INTERVAL_MS)
        }
    }
    return data
}

private fun fetchWeather(lat: Double, lon: Double, zone: ZoneId): WeatherData {
    val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
        "&current=temperature_2m&hourly=temperature_2m&daily=sunset" +
        "&temperature_unit=fahrenheit&forecast_days=2&timezone=auto"
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 15_000
    }
    val body = try {
        connection.inputStream.bufferedReader().use { it.readText() }
    } finally {
        connection.disconnect()
    }

    val root = JSONObject(body)
    val currentTemp = root.getJSONObject("current").getDouble("temperature_2m")

    val hourly = root.getJSONObject("hourly")
    val times = hourly.getJSONArray("time")
    val temps = hourly.getJSONArray("temperature_2m")
    val hourTimes = ArrayList<LocalDateTime>(times.length())
    val hourTemps = ArrayList<Double>(times.length())
    for (i in 0 until times.length()) {
        hourTimes.add(LocalDateTime.parse(times.getString(i)))
        hourTemps.add(temps.getDouble(i))
    }

    // The next sunset at or after now — tonight's, or tomorrow's if tonight's
    // has already passed.
    val now = LocalDateTime.now(zone)
    val sunsets = root.getJSONObject("daily").getJSONArray("sunset")
    var sunset: LocalDateTime? = null
    for (i in 0 until sunsets.length()) {
        val s = LocalDateTime.parse(sunsets.getString(i))
        if (!s.isBefore(now)) { sunset = s; break }
        sunset = s
    }

    val postSunset = sunset?.let { PostSunsetTemp.tempAtHourAfterSunset(hourTimes, hourTemps, it) }

    return WeatherData(
        currentTempF = currentTemp.roundToInt(),
        postSunsetTempF = postSunset?.roundToInt(),
    )
}
