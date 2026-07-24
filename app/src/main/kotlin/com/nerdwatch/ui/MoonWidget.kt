package com.nerdwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
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
 * (facing south: top = overhead, left = rising/east, bottom = underfoot,
 * right = setting/west). A night-cloud is laid over the disk when tonight is
 * ≥50% cloudy — dark where the moon is lit, light where it is in shadow, with a
 * silver-lined edge, the way a cloud looks passing the moon at night.
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
        // Moon disk shrunk 30% within the same sky ring.
        val moonRadius = (ringRadius - scale.px(8f)) * 0.7f

        val litPath = litRegionPath(center, moonRadius, moon.phaseFraction)

        drawMoonDisk(center, moonRadius, litPath, palette)
        if (moon.cloudy) drawNightCloud(center, moonRadius, litPath, palette, scale)
        drawRing(center, ringRadius, palette, scale)
        moon.ringAngleDeg?.let { drawMarker(center, ringRadius, it, palette, scale) }
    }
}

/** Dark disk with the lit phase painted on top. */
private fun DrawScope.drawMoonDisk(center: Offset, r: Float, litPath: Path, palette: AvionicsPalette) {
    drawCircle(color = Color(palette.line), radius = r, center = center)
    drawPath(litPath, Color(palette.fg))
}

/**
 * The illuminated region of the disk, in real coordinates. Built for the bright
 * limb on the right via the terminator ellipse, then mirrored for the waning
 * half of the cycle so the returned path can be used for both fill and clipping.
 */
private fun litRegionPath(center: Offset, r: Float, phaseFraction: Double): Path {
    val k = cos(2.0 * PI * phaseFraction)     // +1 new, 0 quarter, −1 full
    val termRx = (r * abs(k)).toFloat()
    val bulgeRight = k > 0                      // crescent bulges toward the bright limb

    val circleRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    val termRect = Rect(center.x - termRx, center.y - r, center.x + termRx, center.y + r)

    val path = Path().apply {
        moveTo(center.x, center.y - r)
        arcTo(circleRect, -90f, 180f, false)                       // right limb, top→bottom
        if (bulgeRight) arcTo(termRect, 90f, -180f, false)         // terminator via right
        else arcTo(termRect, 90f, 180f, false)                     // terminator via left
        close()
    }

    if (phaseFraction > 0.5) {
        val mirror = Matrix().apply {
            translate(center.x, center.y)
            scale(x = -1f, y = 1f)
            translate(-center.x, -center.y)
        }
        path.transform(mirror)
    }
    return path
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

/**
 * A translucent cloud over part of the disk: light over the moon's shadow, dark
 * over its lit face, with a silver rim — like a cloud drifting past the moon at
 * night. Painting light over the whole disk then dark only over [litPath] yields
 * the inverse-contrast look.
 */
private fun DrawScope.drawNightCloud(
    center: Offset,
    moonRadius: Float,
    litPath: Path,
    palette: AvionicsPalette,
    scale: DesignScale,
) {
    val light = Color.White.copy(alpha = 0.55f)
    val dark = Color.Black.copy(alpha = 0.50f)
    val silver = Color(0xFFCED6E0).copy(alpha = 0.9f)

    val moonPath = Path().apply {
        addOval(Rect(center.x - moonRadius, center.y - moonRadius, center.x + moonRadius, center.y + moonRadius))
    }
    val cloudPath = buildCloudPath(center, moonRadius)

    clipPath(moonPath) { drawPath(cloudPath, light) }   // light over shadow (whole disk)
    clipPath(litPath) { drawPath(cloudPath, dark) }     // dark over the lit face
    clipPath(moonPath) {                                 // silver rim, kept on the disk
        drawPath(cloudPath, silver, style = Stroke(width = scale.px(1.2f)))
    }
}

/** Union of overlapping puffs forming one cloud silhouette across the lower disk. */
private fun buildCloudPath(center: Offset, r: Float): Path {
    val y = center.y + r * 0.28f
    val puffs = listOf(
        Triple(center.x - r * 0.45f, y, r * 0.44f),
        Triple(center.x, y + r * 0.10f, r * 0.55f),
        Triple(center.x + r * 0.48f, y, r * 0.44f),
    )
    var acc = Path().apply { addOval(ovalOf(puffs[0])) }
    for (i in 1 until puffs.size) {
        val next = Path().apply { addOval(ovalOf(puffs[i])) }
        val merged = Path()
        merged.op(acc, next, PathOperation.Union)
        acc = merged
    }
    return acc
}

private fun ovalOf(puff: Triple<Float, Float, Float>): Rect {
    val (cx, cy, radius) = puff
    return Rect(cx - radius, cy - radius, cx + radius, cy + radius)
}
