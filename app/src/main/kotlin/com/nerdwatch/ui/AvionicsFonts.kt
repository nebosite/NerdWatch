package com.nerdwatch.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nerdwatch.R

/**
 * The two bundled type families from the design.
 *
 * Rajdhani carries every numeral; Michroma carries every label. Nothing else
 * should introduce a typeface.
 */
object AvionicsFonts {

    /** Rajdhani 600/700 — time, chrono, timer, data values. */
    val Numerals = FontFamily(
        Font(R.font.rajdhani_semibold, FontWeight.SemiBold),
        Font(R.font.rajdhani_bold, FontWeight.Bold),
    )

    /** Michroma — all stencil labels, always uppercase and letter-spaced. */
    val Stencil = FontFamily(
        Font(R.font.michroma_regular, FontWeight.Normal),
    )
}
