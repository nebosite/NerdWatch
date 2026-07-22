package com.nerdwatch.design

/**
 * The user-tunable constants from the Avionics Mk II design, in one typed place.
 *
 * All sizes are **design pixels** on a 480x480 reference face (Galaxy Watch 6
 * 44mm). [DesignScale] converts them to real dp so the layout holds on any
 * round display. Never hardcode a design pixel anywhere else.
 */
data class AvionicsTokens(
    /** Accent hue used for seconds, active states, brackets and the arc. */
    val accent: Int = 0xFFFFB000.toInt(),
    /** Height of the big HH:MM numerals. */
    val timeFontPx: Float = 110f,
    /** Height of the STEPS / TEMP values. */
    val dataFontPx: Float = 34f,
    /** Width of the data row as a percentage of the content column. */
    val dataAreaWidthPct: Float = 100f,
    /** Height of the battery and date lines. */
    val metaFontPx: Float = 16f,
    /** Gap below the date block. */
    val metaMarginBottomPx: Float = 5f,
) {
    companion object {
        /**
         * The design is authored against a 450px round face. Real hardware
         * differs (44mm Galaxy Watch 6 is 480px, 40mm is 432px), so every size
         * is scaled from this reference rather than assumed — that keeps the
         * designer's proportions instead of letting the layout drift.
         */
        const val REFERENCE_FACE_PX = 450f

        val DEFAULT = AvionicsTokens()
    }
}
