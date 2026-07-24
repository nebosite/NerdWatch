package com.nerdwatch.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
    chronoEngaged: Boolean,
    lightActive: Boolean,
    timerActive: Boolean,
    timerRemaining: String?,
    onChronTap: () -> Unit,
    onChronLongPress: () -> Unit,
    onChronProgress: (Float) -> Unit,
    onLightTap: () -> Unit,
    onTimerTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
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
            modifier = Modifier
                .weight(1f)
                .longPressGesture(
                    // The reset hold only means something once the chrono runs.
                    longPressEnabled = chronoEngaged,
                    scope = scope,
                    onTap = onChronTap,
                    onLongPress = onChronLongPress,
                    onProgress = onChronProgress,
                ),
        )
        UtilityButton(
            label = if (lightActive) "LIGHT·ON" else "LIGHT",
            active = lightActive,
            palette = palette,
            scale = scale,
            shape = RoundedCornerShape(d(0f), d(0f), d(14f), d(14f)),
            modifier = Modifier
                .weight(1f)
                .longPressGesture(
                    longPressEnabled = false,
                    scope = scope,
                    onTap = onLightTap,
                    onLongPress = {},
                    onProgress = {},
                ),
        )
        UtilityButton(
            label = "TIMER",
            active = timerActive,
            palette = palette,
            scale = scale,
            shape = RoundedCornerShape(d(0f), d(0f), d(90f), d(10f)),
            labelInsetEnd = d(23f),
            subLabel = timerRemaining,
            pulse = timerActive,
            modifier = Modifier
                .weight(1f)
                .longPressGesture(
                    longPressEnabled = false,
                    scope = scope,
                    onTap = onTimerTap,
                    onLongPress = {},
                    onProgress = {},
                ),
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
    subLabel: String? = null,
    pulse: Boolean = false,
) {
    val density = LocalDensity.current

    // Spec: a running timer pulses its button brighter. Alpha can't exceed 1, so
    // approximate the 1→2.1→1 brightness swing as a strong alpha pulse.
    val pulseAlpha by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pulse-alpha",
    )
    val liveAlpha = if (pulse) pulseAlpha else 1f

    val labelColor = if (active) Color(palette.accent).copy(alpha = liveAlpha) else Color(palette.dim)
    val borderColor = if (active) Color(palette.accent).copy(alpha = liveAlpha) else Color(palette.line)

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
        // A Column keeps the label pinned at its top padding whether or not the
        // remaining-time sub-label is present, so the face never shifts.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(
                PaddingValues(
                    top = with(density) { scale.px(10f).toDp() },
                    start = labelInsetStart,
                    end = labelInsetEnd,
                ),
            ),
        ) {
            StencilText(
                text = label,
                fontSizePx = scale.px(10f),
                color = labelColor,
                trackingPx = scale.px(2f),
            )
            if (subLabel != null) {
                Spacer(Modifier.height(with(density) { scale.px(4f).toDp() }))
                FixedWidthNumerals(
                    text = subLabel,
                    fontSizePx = scale.px(13f),
                    color = Color(palette.accent).copy(alpha = liveAlpha),
                    weight = FontWeight.SemiBold,
                )
            }
        }
    }
}
