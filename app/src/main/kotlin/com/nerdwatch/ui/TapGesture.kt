package com.nerdwatch.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A plain tap with no ripple and no layout footprint, for making face elements
 * (battery, steps, temp, next event) open their sub-app. Ripple/min-touch-target
 * clickables are avoided deliberately so the face layout never shifts.
 */
fun Modifier.tapGesture(onTap: () -> Unit): Modifier =
    pointerInput(Unit) { detectTapGestures(onTap = { onTap() }) }
