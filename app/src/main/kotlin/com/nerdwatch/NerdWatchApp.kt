package com.nerdwatch

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.nerdwatch.chrono.ChronoFormatter
import com.nerdwatch.chrono.Chronometer
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.AvionicsTokens
import com.nerdwatch.ui.AvionicsFace
import com.nerdwatch.ui.FaceSnapshot
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Root of the NerdWatch app.
 *
 * Owns the chronometer and the long-press arc progress, and ticks the display —
 * ~5Hz idle, ~60Hz while the chrono runs, per the design. Battery, steps,
 * temperature and the next event are still the design's reference values until
 * Increment 5 wires real sources.
 */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val SECONDS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("ss", Locale.US)
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE · MMM d", Locale.US)

private const val IDLE_TICK_MS = 200L
private const val RUNNING_TICK_MS = 16L

@Composable
fun NerdWatchApp() {
    var chrono by remember { mutableStateOf(Chronometer()) }
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var monotonicNow by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var wallNow by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(chrono.isRunning) {
        while (true) {
            monotonicNow = SystemClock.uptimeMillis()
            wallNow = LocalDateTime.now()
            delay(if (chrono.isRunning) RUNNING_TICK_MS else IDLE_TICK_MS)
        }
    }

    val dateText = wallNow.format(DATE_FORMAT).uppercase(Locale.US)

    val snapshot = if (chrono.isEngaged) {
        val parts = ChronoFormatter.format(chrono.elapsedMs(monotonicNow))
        FaceSnapshot.PREVIEW.copy(
            timeText = parts.big,
            secondsText = parts.hundredths,
            dateText = dateText,
            chronoEngaged = true,
        )
    } else {
        FaceSnapshot.PREVIEW.copy(
            timeText = wallNow.format(TIME_FORMAT),
            secondsText = ":" + wallNow.format(SECONDS_FORMAT),
            dateText = dateText,
            chronoEngaged = false,
        )
    }

    AvionicsFace(
        snapshot = snapshot,
        palette = AvionicsPalette.DARK,
        tokens = AvionicsTokens.DEFAULT,
        pressProgress = pressProgress,
        onChronTap = { chrono = chrono.toggle(SystemClock.uptimeMillis()) },
        onChronLongPress = { chrono = chrono.reset() },
        onChronProgress = { pressProgress = it },
    )
}
