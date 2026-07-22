package com.nerdwatch.ui

import android.os.SystemClock
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The design's shared long-press system.
 *
 * - Release before [thresholdMs] is a tap.
 * - At [thresholdMs] the long-press fires **immediately**, not on release.
 * - Progress (0..1) is reported for the arc, but only after [arcDelayMs] so a
 *   quick tap never flashes it.
 *
 * When [longPressEnabled] is false the hold has no meaning (e.g. the chrono is
 * already zeroed): taps still register, but no timer runs and no arc shows.
 *
 * The frame-paced progress loop runs on [scope] rather than the pointer gesture,
 * because a held finger emits no pointer events to tick against and
 * `PointerInputScope` is not itself a `CoroutineScope`.
 */
fun Modifier.longPressGesture(
    longPressEnabled: Boolean,
    scope: CoroutineScope,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onProgress: (Float) -> Unit,
    thresholdMs: Long = 550L,
    arcDelayMs: Long = 100L,
): Modifier = pointerInput(longPressEnabled) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false)
        var longFired = false

        val timer = if (longPressEnabled) {
            scope.launch {
                val start = SystemClock.uptimeMillis()
                while (isActive) {
                    val held = SystemClock.uptimeMillis() - start
                    onProgress(if (held < arcDelayMs) 0f else (held.toFloat() / thresholdMs).coerceAtMost(1f))
                    if (held >= thresholdMs) {
                        longFired = true
                        onProgress(0f)
                        onLongPress()
                        break
                    }
                    withFrameMillis { }
                }
            }
        } else {
            null
        }

        val up = waitForUpOrCancellation()
        timer?.cancel()
        onProgress(0f)

        // A completed press that never reached the threshold is a tap.
        if (!longFired && up != null) onTap()
    }
}
