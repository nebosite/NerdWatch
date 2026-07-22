package com.nerdwatch.design

/**
 * Maps design pixels onto a real display.
 *
 * The Avionics spec is written in pixels against a fixed reference face. Rather
 * than sprinkle magic numbers, every size goes through here, so the same layout
 * holds on a 432px 40mm watch, a 480px 44mm watch, and the emulator.
 *
 * Pure math with no Android types, so it is unit-testable.
 */
class DesignScale(private val faceWidthPx: Float) {

    /** 1.0 on the reference face, <1 on smaller watches, >1 on larger. */
    val factor: Float = faceWidthPx / AvionicsTokens.REFERENCE_FACE_PX

    /** Convert a design pixel measurement to real device pixels. */
    fun px(designPx: Float): Float = designPx * factor

    /** Convert a design em-fraction of a font size to real device pixels. */
    fun em(fraction: Float, designFontPx: Float): Float = px(fraction * designFontPx)

    companion object {
        /** The reference scale, for previews and tests. */
        val REFERENCE = DesignScale(AvionicsTokens.REFERENCE_FACE_PX)
    }
}
