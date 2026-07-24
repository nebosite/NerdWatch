package com.nerdwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalDensity
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import com.nerdwatch.moon.MoonData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The moon widget: the moon in its current phase, wrapped by a light ring whose
 * diamond marker shows where the moon will be in the sky at local midnight
 * (top = overhead, right = rising/east, bottom = underfoot, left = setting/west).
 * A translucent cloud is laid over the disk when tonight is ≥50% cloudy.
 *
 * Drawn on a fixed-size Canvas and positioned absolutely by the caller, so it
 * never moves when the clock switches between 12- and 24-hour format.
 */
@Composable
fun MoonWidget(
    moon: MoonData,
    palette: AvionicsPalette,
    scale: DesignScale,
    diameterDesignPx: Float = 74f,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizeDp = with(density) { scale.px(diameterDesignPx).toDp() }

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val ringRadius = size.minDimension / 2f - scale.px(2f)
        val moonRadius = ringRadius - scale.px(8f)

        drawMoonDisk(center, moonRadius, moon.phaseFraction, palette)
        if (moon.cloudy) drawCloud(center, moonRadius, scale)
        drawRing(center, ringRadius, palette, scale)
        moon.ringAngleDeg?.let { drawMarker(center, ringRadius, it, palette, scale) }
    }
}

/** Dark disk with the lit phase painted on top via the terminator ellipse. */
private fun DrawScope.drawMoonDisk(
    center: Offset,
    r: Float,
    phaseFraction: Double,
    palette: AvionicsPalette,
) {
    val lit = Color(palette.fg)
    val shadow = Color(palette.line)

    drawCircle(color = shadow, radius = r, center = center)

    // Bright-on-right construction; mirror for the waning half of the cycle.
    val k = cos(2.0 * PI * phaseFraction)         // +1 new, 0 quarter, −1 full
    val termRx = (r * abs(k)).toFloat()
    val bulgeRight = k > 0                          // crescent bulges toward the bright limb
    val waning = phaseFraction > 0.5

    val circleRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    val termRect = Rect(center.x - termRx, center.y - r, center.x + termRx, center.y + r)

    val litPath = Path().apply {
        moveTo(center.x, center.y - r)
        arcTo(circleRect, -90f, 180f, false)                          // right limb, top→bottom
        if (bulgeRight) arcTo(termRect, 90f, -180f, false)            // terminator via right
        else arcTo(termRect, 90f, 180f, false)                        // terminator via left
        close()
    }

    if (waning) {
        scale(scaleX = -1f, scaleY = 1f, pivot = center) { drawPath(litPath, lit) }
    } else {
        drawPath(litPath, lit)
    }
}

/** A light ring around the whole thing. */
private fun DrawScope.drawRing(center: Offset, r: Float, palette: AvionicsPalette, scale: DesignScale) {
    drawCircle(
        color = Color(palette.fg).copy(alpha = 0.45f),
        radius = r,
        center = center,
        style = Stroke(width = scale.px(1.5f)),
    )
}

/** A small accent diamond on the ring at [ringAngleDeg] clockwise from top. */
private fun DrawScope.drawMarker(
    center: Offset,
    ringRadius: Float,
    ringAngleDeg: Double,
    palette: AvionicsPalette,
    scale: DesignScale,
) {
    val phi = ringAngleDeg * PI / 180.0
    val cx = center.x + ringRadius * sin(phi).toFloat()
    val cy = center.y - ringRadius * cos(phi).toFloat()
    val h = scale.px(4.5f)

    val diamond = Path().apply {
        moveTo(cx, cy - h)
        lineTo(cx + h, cy)
        lineTo(cx, cy + h)
        lineTo(cx - h, cy)
        close()
    }
    drawPath(diamond, Color(palette.accent))
}

/** A translucent cloud puff over the lower part of the disk. */
private fun DrawScope.drawCloud(center: Offset, moonRadius: Float, scale: DesignScale) {
    val cloud = Color.White.copy(alpha = 0.42f)
    val circleRect = Rect(
        center.x - moonRadius, center.y - moonRadius,
        center.x + moonRadius, center.y + moonRadius,
    )
    clipPath(Path().apply { addOval(circleRect) }) {
        val y = center.y + moonRadius * 0.3f
        drawCircle(cloud, moonRadius * 0.42f, Offset(center.x - moonRadius * 0.4f, y))
        drawCircle(cloud, moonRadius * 0.52f, Offset(center.x, y + moonRadius * 0.08f))
        drawCircle(cloud, moonRadius * 0.42f, Offset(center.x + moonRadius * 0.45f, y))
    }
}
