package com.nerdwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import com.nerdwatch.timer.CountdownTimer

/**
 * The timer preset screen: a 4-per-row grid of minute presets over a BACK
 * return. Shown only when no timer is running, so it never competes with the
 * face for the display.
 */
@Composable
fun TimerPresetScreen(
    palette: AvionicsPalette,
    scale: DesignScale,
    onPick: (minutes: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(palette.bgTop), Color(palette.bgBottom))))
            .padding(horizontal = d(30f)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StencilText(
            text = "TIMER · MINUTES",
            fontSizePx = scale.px(10f),
            color = Color(palette.dim),
            trackingPx = scale.px(4f),
        )
        Spacer(Modifier.height(d(12f)))

        CountdownTimer.PRESET_MINUTES.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(d(12f))) {
                row.forEach { minutes ->
                    StencilButton(
                        text = minutes.toString(),
                        widthPx = 80f,
                        heightPx = 62f,
                        fontSizePx = 26f,
                        palette = palette,
                        scale = scale,
                        onTap = { onPick(minutes) },
                    )
                }
            }
            Spacer(Modifier.height(d(12f)))
        }

        Spacer(Modifier.height(d(6f)))
        StencilButton(
            text = "‹ BACK",
            widthPx = 150f,
            heightPx = 54f,
            fontSizePx = 14f,
            palette = palette,
            scale = scale,
            onTap = onBack,
            numeric = false,
            trackingPx = 3f,
        )
    }
}
