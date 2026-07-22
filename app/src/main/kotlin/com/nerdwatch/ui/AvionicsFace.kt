package com.nerdwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.AvionicsTokens
import com.nerdwatch.design.DesignScale

/**
 * The Avionics Mk II face at rest.
 *
 * Layout is driven entirely by [AvionicsTokens] and [DesignScale] so it holds
 * on any round display. Nothing here owns state — behaviors (chrono, light,
 * timer) drive it by passing a different [FaceSnapshot] and palette.
 */
@Composable
fun AvionicsFace(
    snapshot: FaceSnapshot,
    palette: AvionicsPalette,
    tokens: AvionicsTokens,
    modifier: Modifier = Modifier,
    pressProgress: Float = 0f,
    onChronTap: () -> Unit = {},
    onChronLongPress: () -> Unit = {},
    onChronProgress: (Float) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(palette.bgTop), Color(palette.bgBottom)),
                ),
            ),
    ) {
        val density = LocalDensity.current
        val faceWidthPx = with(density) { maxWidth.toPx() }
        val scale = DesignScale(faceWidthPx)
        fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

        val fg = Color(palette.fg)
        val dim = Color(palette.dim)
        val accent = Color(palette.accent)
        val batteryColor = if (snapshot.batteryIsLow) Color(palette.warn) else dim

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = d(46f),
                    end = d(46f),
                    top = d(46f),
                    bottom = d(108f),
                )
,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d(10f), Alignment.CenterVertically),
        ) {
            BatteryReadout(snapshot.batteryPercent, batteryColor, accent, scale, tokens)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StencilText(
                    text = snapshot.dateText,
                    fontSizePx = scale.px(tokens.metaFontPx),
                    color = fg,
                    trackingPx = scale.px(2f),
                )
                Spacer(Modifier.height(d(tokens.metaMarginBottomPx)))
            }

            Hairline(palette, scale)

            TimeRow(snapshot, palette, tokens, scale)

            if (snapshot.chronoEngaged) {
                StencilText(
                    text = "STOPWATCH · HOLD BTN TO RESET",
                    fontSizePx = scale.px(9f),
                    color = accent,
                    trackingPx = scale.px(3f),
                )
            }

            Hairline(palette, scale)

            DataRow(snapshot, palette, tokens, scale)

            NextEventChip(snapshot, palette, scale)
        }

        UtilityButtonBar(
            palette = palette,
            scale = scale,
            chronoActive = snapshot.chronoEngaged,
            chronoEngaged = snapshot.chronoEngaged,
            lightActive = !palette.glow,
            timerActive = false,
            onChronTap = onChronTap,
            onChronLongPress = onChronLongPress,
            onChronProgress = onChronProgress,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        if (pressProgress > 0f) {
            LongPressArc(color = accent, progress = pressProgress, scale = scale)
        }
    }
}

/**
 * The long-press progress arc. Accent stroke sweeping the top half of the face,
 * from 9 o'clock clockwise across 12 to 3 o'clock — Compose measures angles
 * clockwise from 3 o'clock, so 180° start + a 180° sweep traces exactly that.
 */
@Composable
private fun LongPressArc(color: Color, progress: Float, scale: DesignScale) {
    Canvas(modifier = Modifier.fillMaxSize()) {
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

@Composable
private fun Hairline(palette: AvionicsPalette, scale: DesignScale) {
    val density = LocalDensity.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(with(density) { scale.px(1f).toDp() })
            .background(Color(palette.line)),
    )
}

@Composable
private fun BatteryReadout(
    percent: Int,
    textColor: Color,
    accent: Color,
    scale: DesignScale,
    tokens: AvionicsTokens,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        AccentBar(accent, scale)
        Spacer(Modifier.width(d(10f)))
        StencilText(
            text = "$percent%",
            fontSizePx = scale.px(tokens.metaFontPx),
            color = textColor,
            trackingPx = scale.px(1f),
        )
        Spacer(Modifier.width(d(10f)))
        AccentBar(accent, scale)
    }
}

@Composable
private fun AccentBar(accent: Color, scale: DesignScale) {
    val density = LocalDensity.current
    Box(
        Modifier
            .width(with(density) { scale.px(2f).toDp() })
            .height(with(density) { scale.px(20f).toDp() })
            .background(accent),
    )
}

@Composable
private fun TimeRow(
    snapshot: FaceSnapshot,
    palette: AvionicsPalette,
    tokens: AvionicsTokens,
    scale: DesignScale,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    val secondsFontPx = scale.px(tokens.timeFontPx * 0.3f)
    // Spec: seconds box width locked to 1.7em so it cannot resize as digits change.
    val secondsBoxWidth = with(density) { (secondsFontPx * 1.7f).toDp() }

    // While the chrono runs the numerals switch from fg to accent.
    val digitColor = if (snapshot.chronoEngaged) Color(palette.accent) else Color(palette.timeDigits)

    Row(verticalAlignment = Alignment.Bottom) {
        FixedWidthNumerals(
            text = snapshot.timeText,
            fontSizePx = scale.px(tokens.timeFontPx),
            color = digitColor,
            weight = FontWeight.Bold,
            glowColor = if (palette.glow) Color(palette.accent) else null,
            scaleFactor = scale.factor,
        )

        Spacer(Modifier.width(d(10f)))

        Box(
            modifier = Modifier
                .width(secondsBoxWidth)
                .border(d(1f), Color(palette.line))
                .padding(d(2f)),
            contentAlignment = Alignment.Center,
        ) {
            FixedWidthNumerals(
                text = snapshot.secondsText,
                fontSizePx = secondsFontPx,
                color = Color(palette.accent),
                weight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun DataRow(
    snapshot: FaceSnapshot,
    palette: AvionicsPalette,
    tokens: AvionicsTokens,
    scale: DesignScale,
) {
    Row(
        modifier = Modifier.fillMaxWidth(tokens.dataAreaWidthPct / 100f),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        DataCell("STEPS", snapshot.steps, palette, tokens, scale, borderOnStart = true)
        DataCell("TEMP", snapshot.temperature, palette, tokens, scale, borderOnStart = false)
    }
}

@Composable
private fun DataCell(
    label: String,
    value: String,
    palette: AvionicsPalette,
    tokens: AvionicsTokens,
    scale: DesignScale,
    borderOnStart: Boolean,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    val accentBar = @Composable {
        Box(
            Modifier
                .width(d(2f))
                .height(d(52f))
                .background(Color(palette.accent)),
        )
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (borderOnStart) accentBar()
        Spacer(Modifier.width(d(10f)))

        Column(
            horizontalAlignment = if (borderOnStart) Alignment.Start else Alignment.End,
        ) {
            StencilText(
                text = label,
                fontSizePx = scale.px(14f),
                color = Color(palette.dim),
                trackingPx = scale.px(2f),
                textAlign = if (borderOnStart) TextAlign.Start else TextAlign.End,
            )
            FixedWidthNumerals(
                text = value,
                fontSizePx = scale.px(tokens.dataFontPx),
                color = Color(palette.fg),
                weight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(d(10f)))
        if (!borderOnStart) accentBar()
    }
}

@Composable
private fun NextEventChip(
    snapshot: FaceSnapshot,
    palette: AvionicsPalette,
    scale: DesignScale,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(d(1f), Color(palette.line))
            .background(Color(palette.chip))
            .padding(horizontal = d(12f), vertical = d(7f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StencilText(
            text = "NEXT · ${snapshot.nextEventName}",
            fontSizePx = scale.px(14f),
            color = Color(palette.dim),
            trackingPx = scale.px(1f),
            textAlign = TextAlign.Start,
        )
        FixedWidthNumerals(
            text = snapshot.nextEventCountdown,
            fontSizePx = scale.px(22f),
            color = Color(palette.accent),
            weight = FontWeight.Bold,
        )
    }
}
