package com.nerdwatch.ui

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.AvionicsTokens
import com.nerdwatch.design.DesignScale
import com.nerdwatch.moon.MoonData
import com.nerdwatch.solar.SolarData

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
    onLightTap: () -> Unit = {},
    onTimerTap: () -> Unit = {},
    timerActive: Boolean = false,
    timerRemaining: String? = null,
    onBatteryTap: () -> Unit = {},
    onStepsTap: () -> Unit = {},
    onTempTap: () -> Unit = {},
    onNextTap: () -> Unit = {},
    solar: SolarData = SolarData.UNKNOWN,
    onTimeLongPress: () -> Unit = {},
    onTimeProgress: (Float) -> Unit = {},
    moon: MoonData = MoonData.UNKNOWN,
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
                // The column edge is the DATA row's width; the appointment and
                // the hairline are inset back to 34 individually, so the data row
                // ends up ~8% wider than the appointment. Centered
                // battery/date/time are unaffected.
                .padding(
                    start = d(19f),
                    end = d(19f),
                    top = d(46f),
                    bottom = d(108f),
                )
,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(d(10f), Alignment.CenterVertically),
        ) {
            BatteryReadout(
                snapshot.batteryPercent, batteryColor, accent, scale, tokens,
                modifier = Modifier.tapGesture(onBatteryTap),
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StencilText(
                    text = snapshot.dateText,
                    fontSizePx = scale.px(tokens.metaFontPx),
                    color = fg,
                    trackingPx = scale.px(2f),
                )
                Spacer(Modifier.height(d(tokens.metaMarginBottomPx)))
            }

            TimeRow(
                snapshot, palette, tokens, scale,
                // Long-pressing the clock toggles 12/24h; disabled while the
                // chrono owns the display (its reset is on the CHRON button).
                longPressEnabled = !snapshot.chronoEngaged,
                onLongPress = onTimeLongPress,
                onProgress = onTimeProgress,
            )

            // Inset 15 back to the appointment's 34 total, matching its width.
            Hairline(palette, scale, Modifier.padding(horizontal = d(15f)))

            DataRow(snapshot, palette, tokens, scale, solar, onStepsTap, onTempTap)

            NextEventChip(
                snapshot, palette, scale,
                Modifier.padding(horizontal = d(15f)).tapGesture(onNextTap),
            )
        }

        // Anchored to the face (not the time row), so switching 12/24h — which
        // changes the time's width — never nudges the moon. Sits right of the
        // centered battery/date and above the seconds box.
        MoonWidget(
            moon = moon,
            palette = palette,
            scale = scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                // offset tuned so the (larger) widget's center stays put; the
                // Canvas grew to hold the 2×-diameter cloud.
                .offset(x = d(120f), y = d(87f)),
        )

        UtilityButtonBar(
            palette = palette,
            scale = scale,
            chronoActive = snapshot.chronoEngaged,
            chronoEngaged = snapshot.chronoEngaged,
            lightActive = !palette.glow,
            timerActive = timerActive,
            timerRemaining = timerRemaining,
            onChronTap = onChronTap,
            onChronLongPress = onChronLongPress,
            onChronProgress = onChronProgress,
            onLightTap = onLightTap,
            onTimerTap = onTimerTap,
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        LongPressArcOverlay(color = accent, progress = pressProgress, scale = scale)
    }
}

@Composable
private fun Hairline(palette: AvionicsPalette, scale: DesignScale, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    Box(
        modifier
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
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
    longPressEnabled: Boolean,
    onLongPress: () -> Unit,
    onProgress: (Float) -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    val secondsFontPx = scale.px(tokens.timeFontPx * 0.3f)
    // Spec: seconds box width locked to 1.7em so it cannot resize as digits change.
    val secondsBoxWidth = with(density) { (secondsFontPx * 1.7f).toDp() }

    // While the chrono runs the numerals switch from fg to accent.
    val digitColor = if (snapshot.chronoEngaged) Color(palette.accent) else Color(palette.timeDigits)

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.longPressGesture(
            longPressEnabled = longPressEnabled,
            scope = scope,
            onTap = {},
            onLongPress = onLongPress,
            onProgress = onProgress,
        ),
    ) {
        FixedWidthNumerals(
            text = snapshot.timeText,
            fontSizePx = scale.px(tokens.timeFontPx),
            color = digitColor,
            weight = FontWeight.Bold,
            glowColor = if (palette.glow) Color(palette.accent) else null,
            scaleFactor = scale.factor,
            cellAlignment = Alignment.BottomCenter,
        )

        Spacer(Modifier.width(d(10f)))

        // A tight bordered box around the hundredths, lifted so its baseline
        // meets the main time's. The smaller font's descent sits lower relative
        // to a bottom-aligned row, so the box is nudged up by the measured gap
        // (digits have no descender, so baseline = the visible bottom edge).
        Box(
            modifier = Modifier
                .offset(y = -d(SECONDS_BASELINE_NUDGE))
                .width(secondsBoxWidth)
                .border(d(1f), Color(palette.line))
                .padding(horizontal = d(2f), vertical = d(2f)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            FixedWidthNumerals(
                text = snapshot.secondsText,
                fontSizePx = secondsFontPx,
                color = Color(palette.accent),
                weight = FontWeight.SemiBold,
                cellAlignment = Alignment.BottomCenter,
            )
        }
    }
}

/** Design-px lift of the seconds box so its baseline meets the main time's. */
private const val SECONDS_BASELINE_NUDGE = 7.5f

@Composable
private fun DataRow(
    snapshot: FaceSnapshot,
    palette: AvionicsPalette,
    tokens: AvionicsTokens,
    scale: DesignScale,
    solar: SolarData,
    onStepsTap: () -> Unit,
    onTempTap: () -> Unit,
) {
    // Top-aligned so the STEPS / SOLAR / TEMP labels line up even though the
    // SOLAR cell is taller (it stacks two Kp chips under its label).
    Row(
        modifier = Modifier.fillMaxWidth(tokens.dataAreaWidthPct / 100f),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        DataCell(
            "STEPS", snapshot.steps, palette, tokens, scale, borderOnStart = true,
            modifier = Modifier.tapGesture(onStepsTap),
        )
        SolarCell(solar = solar, palette = palette, scale = scale)
        DataCell(
            "TEMP", snapshot.temperature, palette, tokens, scale, borderOnStart = false,
            modifier = Modifier.tapGesture(onTempTap),
        )
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
    modifier: Modifier = Modifier,
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

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
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
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    fun d(designPx: Float): Dp = with(density) { scale.px(designPx).toDp() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(d(1f), Color(palette.line))
            .background(Color(palette.chip))
            // Taller than the 7px content padding so the gap to the buttons below
            // halves (a centered column moves the chip's bottom down by ~half the
            // added height).
            .padding(horizontal = d(12f), vertical = d(17f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StencilText(
            text = "NEXT · ${snapshot.nextEventName}",
            // 20% smaller than the 14/22 data fonts, per request.
            fontSizePx = scale.px(11.2f),
            color = Color(palette.dim),
            trackingPx = scale.px(1f),
            textAlign = TextAlign.Start,
        )
        FixedWidthNumerals(
            text = snapshot.nextEventCountdown,
            fontSizePx = scale.px(17.6f),
            color = Color(palette.accent),
            weight = FontWeight.Bold,
        )
    }
}
