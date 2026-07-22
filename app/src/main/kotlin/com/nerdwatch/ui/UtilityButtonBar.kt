package com.nerdwatch.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale

/**
 * The three conformal utility buttons pinned to the bottom bezel.
 *
 * Their bottoms deliberately flare past the display edge and are clipped by the
 * round screen, which is why the outer corner radii are so large and asymmetric.
 */
@Composable
fun UtilityButtonBar(
    palette: AvionicsPalette,
    scale: DesignScale,
    chronoActive: Boolean,
    lightActive: Boolean,
    timerActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    Row(
        modifier = modifier.width(d(352f)).height(d(98f)),
        horizontalArrangement = Arrangement.spacedBy(d(8f)),
    ) {
        UtilityButton(
            label = "CHRON",
            active = chronoActive,
            palette = palette,
            scale = scale,
            // CSS order: top-left, top-right, bottom-right, bottom-left.
            shape = RoundedCornerShape(d(0f), d(0f), d(10f), d(90f)),
            labelInsetStart = d(23f),
            modifier = Modifier.weight(1f),
        )
        UtilityButton(
            label = if (lightActive) "LIGHT·ON" else "LIGHT",
            active = lightActive,
            palette = palette,
            scale = scale,
            shape = RoundedCornerShape(d(0f), d(0f), d(14f), d(14f)),
            modifier = Modifier.weight(1f),
        )
        UtilityButton(
            label = "TIMER",
            active = timerActive,
            palette = palette,
            scale = scale,
            shape = RoundedCornerShape(d(0f), d(0f), d(90f), d(10f)),
            labelInsetEnd = d(23f),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun UtilityButton(
    label: String,
    active: Boolean,
    palette: AvionicsPalette,
    scale: DesignScale,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    labelInsetStart: Dp = Dp.Hairline,
    labelInsetEnd: Dp = Dp.Hairline,
) {
    val density = LocalDensity.current
    val labelColor = if (active) Color(palette.accent) else Color(palette.dim)
    // Spec: the border turns accent along with the label while the tool is live.
    val borderColor = if (active) Color(palette.accent) else Color(palette.line)

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(shape)
            .background(Color(palette.chip))
            // The spec omits the bottom border; it falls outside the round
            // display anyway, so a full stroke on this shape reads identically.
            .border(with(density) { scale.px(1f).toDp() }, borderColor, shape),
        contentAlignment = Alignment.TopCenter,
    ) {
        StencilText(
            text = label,
            fontSizePx = scale.px(10f),
            color = labelColor,
            trackingPx = scale.px(2f),
            modifier = Modifier.padding(
                PaddingValues(
                    top = with(density) { scale.px(10f).toDp() },
                    start = labelInsetStart,
                    end = labelInsetEnd,
                ),
            ),
        )
    }
}
