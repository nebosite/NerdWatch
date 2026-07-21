package com.nerdwatch

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.SurfaceHolder
import androidx.wear.watchface.CanvasType
import androidx.wear.watchface.Renderer
import androidx.wear.watchface.WatchState
import androidx.wear.watchface.style.CurrentUserStyleRepository
import java.time.ZonedDateTime

/**
 * Layer 1 (Blank Screen): draws the dial and nothing else.
 *
 * The accent ring exists only so a successful render is visually distinct from a
 * crashed or black screen. Telling the time is Layer 2.
 */
class NerdWatchRenderer(
    surfaceHolder: SurfaceHolder,
    currentUserStyleRepository: CurrentUserStyleRepository,
    watchState: WatchState,
) : Renderer.CanvasRenderer2<NerdWatchRenderer.NerdWatchAssets>(
    surfaceHolder,
    currentUserStyleRepository,
    watchState,
    CanvasType.HARDWARE,
    FRAME_PERIOD_MS,
    clearWithBackgroundTintBeforeRenderingHighlightLayer = false,
) {

    private val palette = NerdWatchPalette.DAY

    private val backgroundPaint = Paint().apply {
        isAntiAlias = true
        color = palette.background
    }

    private val accentPaint = Paint().apply {
        isAntiAlias = true
        color = palette.accent
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    class NerdWatchAssets : SharedAssets {
        override fun onDestroy() = Unit
    }

    override suspend fun createSharedAssets(): NerdWatchAssets = NerdWatchAssets()

    override fun render(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: NerdWatchAssets,
    ) {
        canvas.drawColor(palette.background)

        val radius = (minOf(bounds.width(), bounds.height()) / 2f) - RING_INSET_PX
        canvas.drawCircle(bounds.exactCenterX(), bounds.exactCenterY(), radius, accentPaint)
    }

    override fun renderHighlightLayer(
        canvas: Canvas,
        bounds: Rect,
        zonedDateTime: ZonedDateTime,
        sharedAssets: NerdWatchAssets,
    ) {
        // No complications yet, so there is nothing to highlight.
    }

    private companion object {
        /** One update per second is plenty until there is a running stopwatch. */
        const val FRAME_PERIOD_MS = 1000L
        const val RING_INSET_PX = 12f
    }
}
