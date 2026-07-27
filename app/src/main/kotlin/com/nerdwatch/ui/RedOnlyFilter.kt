package com.nerdwatch.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas

/**
 * Night-vision post-process: when [enabled], renders the whole subtree into an
 * offscreen layer and zeroes the green and blue channels of every pixel, leaving
 * only red (and alpha). Applied at the face root so it catches *all* display
 * colors — palette tokens, the glow, and the moon's hard-coded cloud tints
 * alike — not just those threaded through the palette.
 */
fun Modifier.redOnlyFilter(enabled: Boolean): Modifier =
    if (!enabled) this else this.then(RedOnly)

private val RedOnly: Modifier = Modifier.drawWithCache {
    val paint = Paint().apply {
        colorFilter = ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    1f, 0f, 0f, 0f, 0f, // R' = R
                    0f, 0f, 0f, 0f, 0f, // G' = 0
                    0f, 0f, 0f, 0f, 0f, // B' = 0
                    0f, 0f, 0f, 1f, 0f, // A' = A
                ),
            ),
        )
    }
    onDrawWithContent {
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(Offset.Zero, size), paint)
            drawContent()
            canvas.restore()
        }
    }
}
