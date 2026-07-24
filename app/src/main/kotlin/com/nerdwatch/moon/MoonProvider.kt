package com.nerdwatch.moon

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * Assembles [MoonData]: the phase (from the date, always available), and — when
 * a location is known — the sky-position marker for tonight's midnight and
 * whether tonight is forecast ≥50% cloudy. Without location, the marker and
 * cloud are simply absent.
 */
private const val REFRESH_INTERVAL_MS = 30 * 60_000L
private const val CLOUDY_THRESHOLD = 50.0

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
            val cloudy = location?.let {
                withContext(Dispatchers.IO) {
                    runCatching { isCloudyTonight(it.latitude, it.longitude, zone) }.getOrDefault(false)
                }
            } ?: false

            data = MoonData(phaseFraction = phase, ringAngleDeg = ringAngle, cloudy = cloudy)
            delay(REFRESH_INTERVAL_MS)
        }
    }
    return data
}

/**
 * A location fix: the freshest last-known across providers, or — since
 * last-known is often null on a cold start (and on the emulator) — a one-shot
 * [LocationManager.getCurrentLocation] with a timeout.
 */
private suspend fun currentLocation(context: Context): Location? {
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    if (!granted(Manifest.permission.ACCESS_COARSE_LOCATION) &&
        !granted(Manifest.permission.ACCESS_FINE_LOCATION)
    ) {
        return null
    }

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER,
    )

    providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .maxByOrNull { it.time }
        ?.let { return it }

    val provider = providers.firstOrNull { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        ?: return null

    return withTimeoutOrNull(12_000) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            val executor = Executors.newSingleThreadExecutor()
            runCatching {
                manager.getCurrentLocation(provider, signal, executor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }
    }
}

/** Average cloud cover across tonight's local night hours, thresholded. */
private fun isCloudyTonight(lat: Double, lon: Double, zone: ZoneId): Boolean {
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
    return count > 0 && (sum / count) >= CLOUDY_THRESHOLD
}
