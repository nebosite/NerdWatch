package com.nerdwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale

/**
 * A bordered, chip-filled tappable cell used across the timer screens — the
 * preset minutes, the ± adjusters, and the BACK / FACE returns. Text only, in
 * keeping with the cockpit look.
 */
@Composable
fun StencilButton(
    text: String,
    widthPx: Float,
    heightPx: Float,
    fontSizePx: Float,
    palette: AvionicsPalette,
    scale: DesignScale,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = true,
    trackingPx: Float = 0f,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    Box(
        modifier = modifier
            .size(width = d(widthPx), height = d(heightPx))
            .border(d(1f), Color(palette.line))
            .background(Color(palette.chip))
            .longPressGesture(
                longPressEnabled = false,
                scope = scope,
                onTap = onTap,
                onLongPress = {},
                onProgress = {},
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (numeric) {
            FixedWidthNumerals(
                text = text,
                fontSizePx = scale.px(fontSizePx),
                color = Color(palette.fg),
                weight = FontWeight.Bold,
            )
        } else {
            StencilText(
                text = text,
                fontSizePx = scale.px(fontSizePx),
                color = Color(palette.fg),
                trackingPx = scale.px(trackingPx),
            )
        }
    }
}
