package com.nerdwatch

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Increment 1 wires the live clock only; battery, steps, temperature and the
 * next calendar event are still the design's reference values. They get real
 * sources once the face itself is verified on hardware.
 */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val SECONDS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("ss", Locale.US)
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE · MMM d", Locale.US)

/** Spec: ~5Hz is plenty while idle; the chrono raises this later. */
private const val IDLE_TICK_MS = 200L

@Composable
fun NerdWatchApp() {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(IDLE_TICK_MS)
        }
    }

    val snapshot = FaceSnapshot.PREVIEW.copy(
        timeText = now.format(TIME_FORMAT),
        secondsText = ":" + now.format(SECONDS_FORMAT),
        dateText = now.format(DATE_FORMAT).uppercase(Locale.US),
    )

    AvionicsFace(
        snapshot = snapshot,
        palette = AvionicsPalette.DARK,
        tokens = AvionicsTokens.DEFAULT,
    )
}
