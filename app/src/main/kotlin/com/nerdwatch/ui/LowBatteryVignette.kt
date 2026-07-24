package com.nerdwatch.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * The low-battery warning: a red glow hugging the circle's edge, pulsing
 * 0.35 ↔ 0.95. Drawn on top of whatever screen is showing and purely decorative
 * — with no pointer modifier it never intercepts touches, so the UI beneath
 * stays fully usable.
 */
private const val PEAK_ALPHA = 0.55f
private const val GLOW_INNER_STOP = 0.70f

@Composable
fun LowBatteryVignette(warnColor: Int, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "lowbatt").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "lowbatt-alpha",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f
        val edge = Color(warnColor).copy(alpha = PEAK_ALPHA * pulse)
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    GLOW_INNER_STOP to Color.Transparent,
                    1f to edge,
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius,
            ),
        )
    }
}
