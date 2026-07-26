package com.nerdwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import com.nerdwatch.solar.SolarData
import com.nerdwatch.solar.SolarPalette
import java.util.Locale

/**
 * The SOLAR widget: a "SOLAR" label over two side-by-side Kp chips — current now
 * on the left, the night's forecast peak on the right — each backed by a color
 * that fades with Kp strength (transparent → green → yellow → orange → red →
 * magenta). Sits between STEPS and TEMP; a tap-through to a solar detail screen
 * is a future addition.
 */
@Composable
fun SolarCell(
    solar: SolarData,
    palette: AvionicsPalette,
    scale: DesignScale,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StencilText(
            text = "Kp Index",
            fontSizePx = scale.px(13f),
            color = Color(palette.dim),
            trackingPx = scale.px(1.5f),
        )
        Spacer(Modifier.height(d(3f)))
        Row(horizontalArrangement = Arrangement.spacedBy(d(4f))) {
            KpChip(solar.currentKp, palette, scale)
            KpChip(solar.nightMaxKp, palette, scale)
        }
    }
}

@Composable
private fun KpChip(kp: Double?, palette: AvionicsPalette, scale: DesignScale) {
    val density = LocalDensity.current
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    val background = kp?.let { Color(SolarPalette.backgroundArgb(it)) } ?: Color.Transparent
    val text = kp?.let { String.format(Locale.US, "%.1f", it) } ?: "--"

    Box(
        modifier = Modifier
            .width(d(52f))
            .clip(RoundedCornerShape(d(4f)))
            .background(background)
            .padding(vertical = d(2f)),
        contentAlignment = Alignment.Center,
    ) {
        FixedWidthNumerals(
            text = text,
            // 80% larger than the original 16px, then shrunk 10% per request.
            fontSizePx = scale.px(16f * 1.8f * 0.9f),
            color = Color(palette.fg),
            weight = FontWeight.Bold,
            cellAlignment = Alignment.BottomCenter,
        )
    }
}
