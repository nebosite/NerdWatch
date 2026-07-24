package com.nerdwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale

/**
 * The running-timer screen: big remaining time, ± adjusters, and a return to the
 * face that leaves the timer running. A long-press on the big time cancels the
 * timer (back to presets), using the shared top-half arc.
 */
@Composable
fun TimerRunningScreen(
    remainingBig: String,
    palette: AvionicsPalette,
    scale: DesignScale,
    onAdjust: (deltaMinutes: Int) -> Unit,
    onCancel: () -> Unit,
    onBackToFace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    var pressProgress by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(palette.bgTop), Color(palette.bgBottom)))),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StencilText(
                text = "TIMER",
                fontSizePx = scale.px(10f),
                color = Color(palette.dim),
                trackingPx = scale.px(4f),
            )
            Spacer(Modifier.height(d(6f)))

            // Long-press anywhere on the big readout cancels the timer.
            Box(
                modifier = Modifier.longPressGesture(
                    longPressEnabled = true,
                    scope = scope,
                    onTap = {},
                    onLongPress = onCancel,
                    onProgress = { pressProgress = it },
                ),
                contentAlignment = Alignment.Center,
            ) {
                FixedWidthNumerals(
                    text = remainingBig,
                    fontSizePx = scale.px(123f),
                    color = Color(palette.accent),
                    weight = FontWeight.Bold,
                    glowColor = if (palette.glow) Color(palette.accent) else null,
                    scaleFactor = scale.factor,
                    cellAlignment = Alignment.BottomCenter,
                )
            }

            Spacer(Modifier.height(d(2f)))
            StencilText(
                text = "HOLD TIME TO EXIT",
                fontSizePx = scale.px(8f),
                color = Color(palette.dim),
                trackingPx = scale.px(2f),
            )
            Spacer(Modifier.height(d(14f)))

            Row(horizontalArrangement = Arrangement.spacedBy(d(8f))) {
                listOf(-5, -1, 1, 5).forEach { delta ->
                    StencilButton(
                        text = if (delta > 0) "+$delta" else delta.toString(),
                        widthPx = 74f,
                        heightPx = 58f,
                        fontSizePx = 25f,
                        palette = palette,
                        scale = scale,
                        onTap = { onAdjust(delta) },
                    )
                }
            }

            Spacer(Modifier.height(d(12f)))
            StencilButton(
                text = "‹ FACE",
                widthPx = 150f,
                heightPx = 50f,
                fontSizePx = 14f,
                palette = palette,
                scale = scale,
                onTap = onBackToFace,
                numeric = false,
                trackingPx = 3f,
            )
        }

        LongPressArcOverlay(color = Color(palette.accent), progress = pressProgress, scale = scale)
    }
}
