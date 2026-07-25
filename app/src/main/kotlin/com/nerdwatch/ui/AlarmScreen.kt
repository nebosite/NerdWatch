package com.nerdwatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.nerdwatch.alarm.AlarmFormatter
import com.nerdwatch.alarm.AlarmScale
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * The alarm picker. A 40px logarithmic dial rings the face from the lower-left
 * (1 minute from now) around the top to the lower-right (7 days), with a marker
 * at the chosen time; touching or dragging the dial sets it. The absolute alarm
 * time sits in the middle over the relative offset, and −1 / −5 / SET / +1 / +5
 * sit below. SET schedules and returns to the face.
 */
@Composable
fun AlarmScreen(
    defaultOffsetMinutes: Double,
    baseInstant: Instant,
    baseNow: LocalDateTime,
    palette: AvionicsPalette,
    scale: DesignScale,
    use24Hour: Boolean,
    onSet: (Instant) -> Unit,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    var offset by remember {
        mutableDoubleStateOf(defaultOffsetMinutes.coerceIn(AlarmScale.MIN_MINUTES, AlarmScale.MAX_MINUTES))
    }
    fun nudge(delta: Long) {
        offset = (offset + delta).coerceIn(AlarmScale.MIN_MINUTES, AlarmScale.MAX_MINUTES)
    }

    BackHandler(onBack = onBack)

    val bandWidth = scale.px(40f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(palette.bgTop), Color(palette.bgBottom))))
            .pointerInput(Unit) {
                detectTapGestures { pos -> offsetFromTouch(pos, size.width, size.height, bandWidth)?.let { offset = it } }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    offsetFromTouch(change.position, size.width, size.height, bandWidth)?.let { offset = it }
                }
            },
    ) {
        AlarmDial(offset = offset, palette = palette, bandWidth = bandWidth, scale = scale)

        val alarm = baseNow.plusMinutes(offset.roundToLong())
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            FixedWidthNumerals(
                text = AlarmFormatter.absolute(alarm, baseNow, use24Hour),
                fontSizePx = scale.px(40f),
                color = Color(palette.fg),
                weight = FontWeight.Bold,
                glowColor = if (palette.glow) Color(palette.accent) else null,
                scaleFactor = scale.factor,
                cellAlignment = Alignment.BottomCenter,
            )
            Spacer(Modifier.height(d(2f)))
            StencilText(
                text = AlarmFormatter.relative(offset.roundToLong()),
                fontSizePx = scale.px(11f),
                color = Color(palette.dim),
                trackingPx = scale.px(2f),
            )
            Spacer(Modifier.height(d(14f)))

            Row(horizontalArrangement = Arrangement.spacedBy(d(5f))) {
                StencilButton("-5", 52f, 48f, 22f, palette, scale, onTap = { nudge(-5) })
                StencilButton("-1", 52f, 48f, 22f, palette, scale, onTap = { nudge(-1) })
                StencilButton(
                    "SET", 60f, 48f, 15f, palette, scale, numeric = false, trackingPx = 2f,
                    onTap = { onSet(baseInstant.plusSeconds((offset.roundToLong()) * 60)) },
                )
                StencilButton("+1", 52f, 48f, 22f, palette, scale, onTap = { nudge(1) })
                StencilButton("+5", 52f, 48f, 22f, palette, scale, onTap = { nudge(5) })
            }
        }
    }
}

/** Draws the dial track, a few log tick marks, and the alarm marker. */
@Composable
private fun AlarmDial(offset: Double, palette: AvionicsPalette, bandWidth: Float, scale: DesignScale) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f - scale.px(2f)
        val mid = outer - bandWidth / 2f

        // Track (the 40px band), leaving the 90° bottom gap.
        drawArc(
            color = Color(palette.line),
            startAngle = 135f,           // lower-left in Compose's 3-o'clock-origin convention
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - mid, center.y - mid),
            size = androidx.compose.ui.geometry.Size(mid * 2f, mid * 2f),
            style = Stroke(width = bandWidth),
        )

        // Reference ticks at 1m, 1h, 1d, 7d.
        listOf(1.0, 60.0, 1440.0, 10080.0).forEach { minutes ->
            drawRadialMark(center, outer, bandWidth, AlarmScale.fraction(minutes), Color(palette.dim), scale.px(1.5f))
        }

        // The alarm marker.
        drawRadialMark(center, outer, bandWidth, AlarmScale.fraction(offset), Color(palette.accent), scale.px(4f))
    }
}

/** A radial bar across the band at a dial fraction. */
private fun DrawScope.drawRadialMark(
    center: Offset,
    outer: Float,
    bandWidth: Float,
    fraction: Double,
    color: Color,
    strokeWidth: Float,
) {
    val angleFromEast = (AlarmScale.angleForFraction(fraction) - 90.0) * PI / 180.0
    val cosA = cos(angleFromEast).toFloat()
    val sinA = sin(angleFromEast).toFloat()
    val inner = outer - bandWidth
    drawLine(
        color = color,
        start = Offset(center.x + inner * cosA, center.y + inner * sinA),
        end = Offset(center.x + outer * cosA, center.y + outer * sinA),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

/**
 * Minutes-from-now for a touch on the dial, or null when the touch is in the
 * inner (buttons/time) area rather than on the band.
 */
private fun offsetFromTouch(pos: Offset, width: Int, height: Int, bandWidth: Float): Double? {
    val cx = width / 2f
    val cy = height / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val distance = hypot(dx, dy)
    val outer = minOf(width, height) / 2f
    // Only the band (and a little inward) drives the dial; deeper touches are for
    // the centered value and buttons.
    if (distance < outer - bandWidth - 24f) return null

    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0   // clockwise from top
    if (angle < 0) angle += 360.0
    return AlarmScale.offsetMinutes(AlarmScale.fractionForAngle(angle))
}
