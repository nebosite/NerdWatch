package com.nerdwatch.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * A single location fix, shared by every widget that needs one (the moon marker,
 * the cloud cover, the weather temps).
 *
 * Returns the freshest last-known fix across providers, or — since last-known is
 * often null on a cold start (and on the emulator) — a one-shot
 * [LocationManager.getCurrentLocation] with a timeout. Null when no location
 * permission is held or no provider is enabled.
 */
suspend fun currentLocation(context: Context): Location? {
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
