package com.nerdwatch.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.nerdwatch.R
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale
import com.nerdwatch.moon.MoonData
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The moon widget: the moon in its current phase, hugged by a light sky ring
 * whose diamond marker shows where the moon will be in the sky at local midnight
 * (facing south: top = overhead, left = rising/east, bottom = underfoot,
 * right = setting/west). One drifting cloud is drawn when tonight's forecast
 * cover is over 30%, a second (down and to the right) when it is over 60%: light
 * over the moon's shadow, dark amber (the buttons' fill) over its lit face, with
 * a silver-lined edge, from the supplied cloud images.
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
    val cloudImage = ImageBitmap.imageResource(R.drawable.cloud)
    val cloudHighlight = ImageBitmap.imageResource(R.drawable.cloud_highlight)

    Canvas(modifier = modifier.size(sizeDp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val moonRadius = scale.px(18f)
        val ringRadius = moonRadius + scale.px(4f)   // ring hugs the (already shrunk) moon

        val litPath = litRegionPath(center, moonRadius, moon.phaseFraction)

        drawMoonDisk(center, moonRadius, litPath, palette)
        drawRing(center, ringRadius, palette, scale)
        if (moon.cloudCount >= 1) {
            drawNightCloud(center, moonRadius, litPath, palette, cloudImage, cloudHighlight, moon.cloudCount)
        }
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
 * The night cloud, drawn from the supplied cloud image: tinted light over the
 * sky and the moon's shadow, tinted dark amber — the buttons' fill — over the
 * lit face, with the highlight image laid on top for the silver lining. Sized
 * 50% wider than the moon so it overhangs the disk on both sides.
 */
private fun DrawScope.drawNightCloud(
    center: Offset,
    moonRadius: Float,
    litPath: Path,
    palette: AvionicsPalette,
    cloud: ImageBitmap,
    highlight: ImageBitmap,
    count: Int,
) {
    val light = Color(0xFFC2CAD6)                        // soft light cloud
    val darkAmber = Color(buttonFill(palette))          // matches the buttons' background
    val silver = Color(0xFFDCE2EC)

    // Base cloud = 1.5× the moon width, then stretched 50% wider and 2× taller.
    val cloudWidth = moonRadius * 3f * 1.5f
    val cloudHeight = (moonRadius * 3f * cloud.height / cloud.width) * 2f
    // Grow the extra height downward: keep the original top edge.
    val topY = center.y - (moonRadius * 3f * cloud.height / cloud.width) / 2f

    fun drawCloudAt(cloudCenterX: Float, cloudTop: Float) {
        val left = cloudCenterX - cloudWidth / 2f
        fun paint(image: ImageBitmap, color: Color, alpha: Float) {
            drawImage(
                image = image,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(image.width, image.height),
                dstOffset = IntOffset(left.roundToInt(), cloudTop.roundToInt()),
                dstSize = IntSize(cloudWidth.roundToInt(), cloudHeight.roundToInt()),
                alpha = alpha,
                colorFilter = ColorFilter.tint(color),
            )
        }
        paint(cloud, light, 0.85f)                              // light over sky + shadow
        clipPath(litPath) { paint(cloud, darkAmber, 0.95f) }    // dark amber over the lit face
        paint(highlight, silver, 0.9f)                          // silver highlights over sky + shadow
        clipPath(litPath) { paint(highlight, darkAmber, 0.9f) } // dark highlights over the lit face
    }

    drawCloudAt(center.x, topY)
    // A second copy, a little down and to the right, only when it is cloudier.
    if (count >= 2) drawCloudAt(center.x + moonRadius * 0.6f, topY + moonRadius * 0.6f)
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
