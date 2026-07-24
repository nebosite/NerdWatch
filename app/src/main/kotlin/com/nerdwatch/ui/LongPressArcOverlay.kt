package com.nerdwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.nerdwatch.design.DesignScale

/**
 * The long-press progress arc. Accent stroke sweeping the top half of the face,
 * from 9 o'clock clockwise across 12 to 3 o'clock — Compose measures angles
 * clockwise from 3 o'clock, so a 180° start plus a 180° sweep traces exactly
 * that. Shared by the main face (chrono reset) and the timer running screen
 * (timer cancel).
 */
@Composable
fun LongPressArcOverlay(
    color: Color,
    progress: Float,
    scale: DesignScale,
    modifier: Modifier = Modifier,
) {
    if (progress <= 0f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val stroke = scale.px(6f)
        val inset = scale.px(3f) + stroke / 2f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}
