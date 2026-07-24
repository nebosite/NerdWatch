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
 * The moon widget: the moon in its current phase, hugged by a light sky ring
 * whose diamond marker shows where the moon will be in the sky at local midnight
 * (facing south: top = overhead, left = rising/east, bottom = underfoot,
 * right = setting/west). When tonight is ≥50% cloudy a romantic cloud — twice
 * the moon's diameter, widest at its base and tapering to points at each side —
 * drifts across: light over the moon's shadow, dark amber (the buttons' fill)
 * over its lit face, with a silver-lined edge.
 *
 * Drawn on a fixed-size Canvas and positioned absolutely by the caller, so it
 * never moves when the clock switches between 12- and 24-hour format.
 */
@Composable
fun MoonWidget(
    moon: MoonData,
    palette: AvionicsPalette,
    scale: DesignScale,
    diameterDesignPx: Float = 84f,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val sizeDp = with(density) { scale.px(diameterDesignPx).toDp() }

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val moonRadius = scale.px(18f)
        val ringRadius = moonRadius + scale.px(4f)   // ring hugs the (already shrunk) moon

        val litPath = litRegionPath(center, moonRadius, moon.phaseFraction)

        drawMoonDisk(center, moonRadius, litPath, palette)
        drawRing(center, ringRadius, palette, scale)
        if (moon.cloudy) drawNightCloud(center, moonRadius, litPath, palette, scale)
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
 * half so the path can be used for both fill and clipping.
 */
private fun litRegionPath(center: Offset, r: Float, phaseFraction: Double): Path {
    val k = cos(2.0 * PI * phaseFraction)
    val termRx = (r * abs(k)).toFloat()
    val bulgeRight = k > 0

    val circleRect = Rect(center.x - r, center.y - r, center.x + r, center.y + r)
    val termRect = Rect(center.x - termRx, center.y - r, center.x + termRx, center.y + r)

    val path = Path().apply {
        moveTo(center.x, center.y - r)
        arcTo(circleRect, -90f, 180f, false)
        if (bulgeRight) arcTo(termRect, 90f, -180f, false) else arcTo(termRect, 90f, 180f, false)
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

/** A light ring hugging the moon. */
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
 * The night cloud: light over the whole cloud (soft over the sky and the moon's
 * shadow), dark amber — the buttons' fill color — over the lit face, and a
 * silver rim. Twice the moon's diameter, widest at the base, pointed at the ends.
 */
private fun DrawScope.drawNightCloud(
    center: Offset,
    moonRadius: Float,
    litPath: Path,
    palette: AvionicsPalette,
    scale: DesignScale,
) {
    val light = Color.White.copy(alpha = 0.50f)
    val darkAmber = Color(buttonFill(palette))          // matches the buttons' background
    val silver = Color(0xFFCED6E0).copy(alpha = 0.9f)

    val cloudPath = romanticCloud(center, moonRadius)

    drawPath(cloudPath, light)                           // light over sky + shadow
    clipPath(litPath) { drawPath(cloudPath, darkAmber) } // dark amber over the lit face
    drawPath(cloudPath, silver, style = Stroke(width = scale.px(1.2f)))
}

/**
 * A cloud silhouette, width = 2× the moon's diameter, with a flat wide base,
 * a bumpy top and sharp points at the far left and right.
 */
private fun romanticCloud(center: Offset, moonRadius: Float): Path {
    val hw = moonRadius * 2f                  // half-width → full width = 4·r = 2 diameters
    val cx = center.x
    val baseY = center.y + moonRadius * 0.75f
    val leftX = cx - hw
    val rightX = cx + hw
    // Rounded bumps ride along a shoulder line above the base; none dominates,
    // so the top reads as a lumpy cloud rather than a single peak.
    val shoulder = center.y - moonRadius * 0.45f
    val bump = moonRadius * 0.42f

    return Path().apply {
        moveTo(leftX, baseY)                                                   // left point
        quadraticBezierTo(cx - hw * 0.86f, shoulder + bump, cx - hw * 0.66f, shoulder)
        quadraticBezierTo(cx - hw * 0.54f, shoulder - bump, cx - hw * 0.38f, shoulder)
        quadraticBezierTo(cx - hw * 0.24f, shoulder - bump * 1.25f, cx - hw * 0.08f, shoulder)
        quadraticBezierTo(cx + hw * 0.06f, shoulder - bump * 1.2f, cx + hw * 0.22f, shoulder)
        quadraticBezierTo(cx + hw * 0.38f, shoulder - bump, cx + hw * 0.54f, shoulder)
        quadraticBezierTo(cx + hw * 0.72f, shoulder - bump * 0.7f, cx + hw * 0.84f, shoulder)
        quadraticBezierTo(cx + hw * 0.96f, shoulder + bump, rightX, baseY)     // right point
        close()                                                                // flat, widest base
    }
}

/** The buttons' apparent fill: their chip color composited over the face background. */
private fun buttonFill(palette: AvionicsPalette): Int {
    val chip = palette.chip
    val bg = palette.bgBottom
    val a = ((chip ushr 24) and 0xFF) / 255f
    fun channel(shift: Int): Int {
        val f = (chip shr shift) and 0xFF
        val b = (bg shr shift) and 0xFF
        return (a * f + (1 - a) * b).toInt().coerceIn(0, 255)
    }
    return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
