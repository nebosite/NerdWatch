package com.nerdwatch.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Live readings from the watch. Each returns null (or a sentinel) when the
 * source is unavailable so the caller can fall back to a placeholder — the
 * emulator has no step counter, for instance.
 */

/** Battery charge as a 0..100 percentage, from the sticky battery broadcast. */
@Composable
fun rememberBatteryPercent(): Int {
    val context = LocalContext.current
    var percent by remember { mutableIntStateOf(readBatteryPercent(context)) }

    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                percentFrom(intent)?.let { percent = it }
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        percentFrom(sticky)?.let { percent = it }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return percent
}

/**
 * Cumulative step count from the hardware step counter, or null when the sensor
 * is absent (e.g. the emulator) or the ACTIVITY_RECOGNITION permission is not
 * granted. Real daily-step aggregation is a Health Services job for later; this
 * is the honest best-effort until the watch is in hand.
 */
@Composable
fun rememberStepCount(): Int? {
    val context = LocalContext.current
    var steps by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = manager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                steps = event.values.firstOrNull()?.toInt()
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) {
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager?.unregisterListener(listener) }
    }
    return steps
}

private fun readBatteryPercent(context: Context): Int {
    val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    return percentFrom(sticky) ?: 0
}

private fun percentFrom(intent: Intent?): Int? {
    intent ?: return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level >= 0 && scale > 0) level * 100 / scale else null
}
