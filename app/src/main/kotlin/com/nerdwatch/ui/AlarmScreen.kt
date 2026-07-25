package com.nerdwatch.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.nerdwatch.alarm.AlarmNavigation
import com.nerdwatch.alarm.AlarmScale
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToLong
import kotlin.math.sin

/**
 * The alarm picker / manager. A 40px logarithmic dial rings the face (lower-left
 * = 1 minute from now, over the top, to lower-right = 7 days); touch or drag it
 * to set the time. The chosen alarm sits in the middle, flanked by the previous
 * and next active alarms in small text — swipe to bring a neighbour into the
 * middle to edit or clear it. ADD schedules (or updates) it and returns to the
 * face; CLEAR deletes it and shows CLEARED (touch the dial to set a new one);
 * BACK returns without changes.
 */
private const val SWIPE_THRESHOLD = 40f

@Composable
fun AlarmScreen(
    defaultOffsetMinutes: Double,
    baseInstant: Instant,
    baseNow: LocalDateTime,
    alarms: List<Instant>,
    palette: AvionicsPalette,
    scale: DesignScale,
    use24Hour: Boolean,
    onAdd: (original: Instant?, at: Instant) -> Unit,
    onClear: (original: Instant) -> Unit,
    onBack: () -> Unit,
) {
    val density = LocalDensity.current
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }
    val zone = ZoneId.systemDefault()

    // The slot being shown: its offset (null = CLEARED) and, if it came from an
    // existing alarm, that alarm's instant (so ADD updates and CLEAR deletes it).
    var offset by remember {
        mutableStateOf<Double?>(defaultOffsetMinutes.coerceIn(AlarmScale.MIN_MINUTES, AlarmScale.MAX_MINUTES))
    }
    var original by remember { mutableStateOf<Instant?>(null) }

    fun slotInstant(): Instant? = offset?.let { baseInstant.plusSeconds(it.roundToLong() * 60) }
    fun offsetOf(alarm: Instant): Double =
        ((alarm.epochSecond - baseInstant.epochSecond) / 60.0).coerceIn(AlarmScale.MIN_MINUTES, AlarmScale.MAX_MINUTES)
    fun others(): List<Instant> = alarms.filter { it != original }
    fun localOf(alarm: Instant): LocalDateTime = LocalDateTime.ofInstant(alarm, zone)

    fun nudge(delta: Long) {
        val current = offset ?: 0.0
        offset = (current + delta).coerceIn(AlarmScale.MIN_MINUTES, AlarmScale.MAX_MINUTES)
    }
    fun swipeToNext() {
        AlarmNavigation.next(others(), slotInstant() ?: baseInstant)?.let { original = it; offset = offsetOf(it) }
    }
    fun swipeToPrevious() {
        AlarmNavigation.previous(others(), slotInstant() ?: baseInstant)?.let { original = it; offset = offsetOf(it) }
    }

    BackHandler(onBack = onBack)

    val bandWidth = scale.px(40f)
    val prev = AlarmNavigation.previous(others(), slotInstant() ?: baseInstant)
    val next = AlarmNavigation.next(others(), slotInstant() ?: baseInstant)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(palette.bgTop), Color(palette.bgBottom)))),
    ) {
        // The dial owns its own tap/drag (gated to the band), so it never
        // competes with the centre's horizontal swipe navigation.
        AlarmDial(
            offset = offset,
            palette = palette,
            bandWidth = bandWidth,
            scale = scale,
            onSet = { offset = it },
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Peeks flank a centered value; a horizontal swipe navigates alarms.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = d(48f))
                    .pointerInput(alarms, original, offset) {
                        var dx = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (dx <= -SWIPE_THRESHOLD) swipeToNext() else if (dx >= SWIPE_THRESHOLD) swipeToPrevious()
                                dx = 0f
                            },
                            onHorizontalDrag = { _, amount -> dx += amount },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                prev?.let {
                    StencilText(
                        AlarmFormatter.peek(localOf(it), use24Hour), scale.px(12f), Color(palette.dim), scale.px(1f),
                        modifier = Modifier.align(Alignment.CenterStart),
                    )
                }
                val slotLocal = offset?.let { baseNow.plusMinutes(it.roundToLong()) }
                if (slotLocal != null) {
                    FixedWidthNumerals(
                        text = AlarmFormatter.absolute(slotLocal, baseNow, use24Hour),
                        fontSizePx = scale.px(38f),
                        color = Color(palette.fg),
                        weight = FontWeight.Bold,
                        glowColor = if (palette.glow) Color(palette.accent) else null,
                        scaleFactor = scale.factor,
                        cellAlignment = Alignment.BottomCenter,
                    )
                } else {
                    StencilText("CLEARED", scale.px(24f), Color(palette.accent), scale.px(3f))
                }
                next?.let {
                    StencilText(
                        AlarmFormatter.peek(localOf(it), use24Hour), scale.px(12f), Color(palette.dim), scale.px(1f),
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            Spacer(Modifier.height(d(2f)))
            StencilText(
                text = offset?.let { AlarmFormatter.relative(it.roundToLong()) } ?: "TOUCH DIAL TO SET",
                fontSizePx = scale.px(11f),
                color = Color(palette.dim),
                trackingPx = scale.px(2f),
            )
            Spacer(Modifier.height(d(12f)))

            Row(horizontalArrangement = Arrangement.spacedBy(d(5f))) {
                StencilButton("-5", 50f, 44f, 21f, palette, scale, onTap = { nudge(-5) })
                StencilButton("-1", 50f, 44f, 21f, palette, scale, onTap = { nudge(-1) })
                StencilButton(
                    "ADD", 58f, 44f, 15f, palette, scale, numeric = false, trackingPx = 2f,
                    onTap = { slotInstant()?.let { onAdd(original, it) } },
                )
                StencilButton("+1", 50f, 44f, 21f, palette, scale, onTap = { nudge(1) })
                StencilButton("+5", 50f, 44f, 21f, palette, scale, onTap = { nudge(5) })
            }
            Spacer(Modifier.height(d(6f)))
            Row(horizontalArrangement = Arrangement.spacedBy(d(6f))) {
                StencilButton(
                    "CLEAR", 100f, 40f, 13f, palette, scale, numeric = false, trackingPx = 2f,
                    onTap = {
                        original?.let { onClear(it) }
                        original = null
                        offset = null
                    },
                )
                StencilButton(
                    "BACK", 100f, 40f, 13f, palette, scale, numeric = false, trackingPx = 2f,
                    onTap = onBack,
                )
            }
        }
    }
}

/** Draws the dial track, a few log tick marks, and the alarm marker (if set). */
@Composable
private fun AlarmDial(
    offset: Double?,
    palette: AvionicsPalette,
    bandWidth: Float,
    scale: DesignScale,
    onSet: (Double) -> Unit,
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { pos -> offsetFromTouch(pos, size.width, size.height, bandWidth)?.let(onSet) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    offsetFromTouch(change.position, size.width, size.height, bandWidth)?.let(onSet)
                }
            },
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension / 2f - scale.px(2f)
        val mid = outer - bandWidth / 2f

        drawArc(
            color = Color(palette.line),
            startAngle = 135f,           // lower-left in Compose's 3-o'clock-origin convention
            sweepAngle = 270f,
            useCenter = false,
            topLeft = Offset(center.x - mid, center.y - mid),
            size = androidx.compose.ui.geometry.Size(mid * 2f, mid * 2f),
            style = Stroke(width = bandWidth),
        )

        listOf(1.0, 60.0, 1440.0, 10080.0).forEach { minutes ->
            drawRadialMark(center, outer, bandWidth, AlarmScale.fraction(minutes), Color(palette.dim), scale.px(1.5f))
        }

        offset?.let {
            drawRadialMark(center, outer, bandWidth, AlarmScale.fraction(it), Color(palette.accent), scale.px(4f))
        }
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
 * inner (peeks/buttons) area rather than on the band.
 */
private fun offsetFromTouch(pos: Offset, width: Int, height: Int, bandWidth: Float): Double? {
    val cx = width / 2f
    val cy = height / 2f
    val dx = pos.x - cx
    val dy = pos.y - cy
    val distance = hypot(dx, dy)
    val outer = minOf(width, height) / 2f
    if (distance < outer - bandWidth - 24f) return null

    var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0   // clockwise from top
    if (angle < 0) angle += 360.0
    return AlarmScale.offsetMinutes(AlarmScale.fractionForAngle(angle))
}
