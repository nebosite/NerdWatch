package com.nerdwatch

import android.content.Context
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import com.nerdwatch.chrono.ChronoFormatter
import com.nerdwatch.chrono.Chronometer
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.AvionicsTokens
import com.nerdwatch.design.DesignScale
import com.nerdwatch.timer.CountdownTimer
import com.nerdwatch.timer.TimerFormatter
import com.nerdwatch.ui.AvionicsFace
import com.nerdwatch.ui.FaceSnapshot
import com.nerdwatch.ui.TimeUpOverlay
import com.nerdwatch.ui.TimerPresetScreen
import com.nerdwatch.ui.TimerRunningScreen
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Root of the NerdWatch app: owns all state and routes between the face and the
 * timer screens. The main face's layout never changes — the timer lives on its
 * own screens, and only color / overlay state is fed back to the face.
 */
private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
private val SECONDS_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("ss", Locale.US)
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE · MMM d", Locale.US)

private const val IDLE_TICK_MS = 200L
private const val RUNNING_TICK_MS = 16L

private enum class Screen { FACE, TIMER_PRESET, TIMER_RUNNING }

@Composable
fun NerdWatchApp() {
    var chrono by remember { mutableStateOf(Chronometer()) }
    var timer by remember { mutableStateOf(CountdownTimer()) }
    var screen by remember { mutableStateOf(Screen.FACE) }
    var timeUp by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableFloatStateOf(0f) }
    var lightOn by remember { mutableStateOf(false) }
    var monotonicNow by remember { mutableLongStateOf(SystemClock.uptimeMillis()) }
    var wallNow by remember { mutableStateOf(LocalDateTime.now()) }

    val context = LocalContext.current

    // Fast ticks while either the chrono or a timer is live; idle otherwise.
    val needsFastTick = chrono.isRunning || timer.isRunning(monotonicNow)
    LaunchedEffect(needsFastTick) {
        while (true) {
            monotonicNow = SystemClock.uptimeMillis()
            wallNow = LocalDateTime.now()
            delay(if (needsFastTick) RUNNING_TICK_MS else IDLE_TICK_MS)
        }
    }

    // Fire once when the timer reaches zero: buzz and raise the time-up flash.
    LaunchedEffect(timer.hasFired(monotonicNow)) {
        if (timer.isSet() && timer.hasFired(monotonicNow) && !timeUp) {
            timeUp = true
            vibrate(context)
        }
    }

    // Flashlight mode keeps the screen awake; cleared when it turns off.
    val view = LocalView.current
    DisposableEffect(lightOn) {
        view.keepScreenOn = lightOn
        onDispose { view.keepScreenOn = false }
    }

    val palette = if (lightOn) AvionicsPalette.LIGHT else AvionicsPalette.DARK
    val dateText = wallNow.format(DATE_FORMAT).uppercase(Locale.US)
    val timerRunning = timer.isRunning(monotonicNow)

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

    BoxWithConstraints {
        val density = LocalDensity.current
        val scale = DesignScale(with(density) { maxWidth.toPx() })

        when (screen) {
            Screen.FACE -> AvionicsFace(
                snapshot = snapshot,
                palette = palette,
                tokens = AvionicsTokens.DEFAULT,
                pressProgress = pressProgress,
                onChronTap = { chrono = chrono.toggle(SystemClock.uptimeMillis()) },
                onChronLongPress = { chrono = chrono.reset() },
                onChronProgress = { pressProgress = it },
                onLightTap = { lightOn = !lightOn },
                onTimerTap = {
                    screen = if (timerRunning) Screen.TIMER_RUNNING else Screen.TIMER_PRESET
                },
                timerActive = timerRunning,
                timerRemaining = if (timerRunning) TimerFormatter.compact(timer.remainingMs(monotonicNow)) else null,
            )

            Screen.TIMER_PRESET -> TimerPresetScreen(
                palette = palette,
                scale = scale,
                onPick = { minutes ->
                    timer = timer.startMinutes(minutes, SystemClock.uptimeMillis())
                    screen = Screen.TIMER_RUNNING
                },
                onBack = { screen = Screen.FACE },
            )

            Screen.TIMER_RUNNING -> TimerRunningScreen(
                remainingBig = TimerFormatter.big(timer.remainingMs(monotonicNow)),
                palette = palette,
                scale = scale,
                onAdjust = { delta -> timer = timer.adjustMinutes(delta, SystemClock.uptimeMillis()) },
                onCancel = {
                    timer = timer.cancel()
                    screen = Screen.TIMER_PRESET
                },
                onBackToFace = { screen = Screen.FACE },
            )
        }

        if (timeUp) {
            TimeUpOverlay(
                palette = palette,
                scale = scale,
                onDismiss = {
                    timeUp = false
                    timer = timer.cancel()
                    screen = Screen.FACE
                },
            )
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    vibrator.vibrate(VibrationEffect.createOneShot(600, VibrationEffect.DEFAULT_AMPLITUDE))
}
