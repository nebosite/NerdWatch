package com.nerdwatch.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Text

/**
 * Numerals laid out in fixed-width cells so the readout NEVER shifts as digits
 * change — the design's single most important rule.
 *
 * Each digit occupies exactly 0.52em and each `:` or `.` exactly 0.28em, with
 * the glyph centered in its cell. Proportional glyph advance is therefore
 * irrelevant: a `1` reserves the same space as an `8`.
 */
private const val DIGIT_CELL_EM = 0.52f
private const val PUNCT_CELL_EM = 0.28f

/**
 * Spec line-height. This must be an explicit cell height, not just a TextStyle
 * lineHeight: `Trim.Both` will not shrink a line below the font's natural
 * metrics, and Rajdhani's ascent+descent is ~1.28em — which measured 150px
 * instead of 96px and squeezed the next-event chip to zero height.
 */
private const val CELL_HEIGHT_EM = 0.82f

/** Separators that sit between sliding digits and so must not shift either. */
private val LOCKED_PUNCTUATION = setOf(':', '.')

/** Design spec: soft glow at blur 18px @ 40%, plus a tight blur 3px @ 67%. */
private const val GLOW_WIDE_BLUR_PX = 18f
private const val GLOW_TIGHT_BLUR_PX = 3f
private const val GLOW_WIDE_ALPHA = 0.40f
private const val GLOW_TIGHT_ALPHA = 0.67f

@Composable
fun FixedWidthNumerals(
    text: String,
    fontSizePx: Float,
    color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Bold,
    glowColor: Color? = null,
    scaleFactor: Float = 1f,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (glowColor != null) {
            // Unbounded, or the blur is clipped to the layout box and paints a
            // hard-edged rectangle behind the numerals instead of a halo.
            NumeralRow(
                text = text,
                fontSizePx = fontSizePx,
                color = glowColor.copy(alpha = GLOW_WIDE_ALPHA),
                weight = weight,
                modifier = Modifier.blur(
                    (GLOW_WIDE_BLUR_PX * scaleFactor).dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                ),
            )
            NumeralRow(
                text = text,
                fontSizePx = fontSizePx,
                color = glowColor.copy(alpha = GLOW_TIGHT_ALPHA),
                weight = weight,
                modifier = Modifier.blur(
                    (GLOW_TIGHT_BLUR_PX * scaleFactor).dp,
                    edgeTreatment = BlurredEdgeTreatment.Unbounded,
                ),
            )
        }

        NumeralRow(text = text, fontSizePx = fontSizePx, color = color, weight = weight)
    }
}

@Composable
private fun NumeralRow(
    text: String,
    fontSizePx: Float,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        text.forEach { glyph ->
            // Only digits and the separators they slide past need locked cells.
            // Letters (the T-2H 37M countdown, the ° on temperature) keep their
            // natural advance, or a 0.28em cell crushes them together.
            val cellEm = when {
                glyph.isDigit() -> DIGIT_CELL_EM
                glyph in LOCKED_PUNCTUATION -> PUNCT_CELL_EM
                else -> null
            }
            val cellHeightDp = with(density) { (CELL_HEIGHT_EM * fontSizePx).toDp() }
            val cellModifier = if (cellEm == null) {
                Modifier.height(cellHeightDp)
            } else {
                Modifier
                    .width(with(density) { (cellEm * fontSizePx).toDp() })
                    .height(cellHeightDp)
            }

            Box(
                modifier = cellModifier,
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = glyph.toString(),
                    color = color,
                    textAlign = TextAlign.Center,
                    // Draws centered and may overflow the cell visually, but
                    // never claims more layout space than the cell.
                    modifier = Modifier.wrapContentHeight(unbounded = true),
                    style = TextStyle(
                        fontFamily = AvionicsFonts.Numerals,
                        fontWeight = weight,
                        fontSize = with(density) { fontSizePx.toSp() },
                        // Spec: line-height 0.82 keeps the numerals optically tight.
                        // Without trimming the font padding and line-height slack,
                        // each row reserves ~1.3em and shoves the chip off the face.
                        lineHeight = with(density) { (fontSizePx * 0.82f).toSp() },
                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Center,
                            trim = LineHeightStyle.Trim.Both,
                        ),
                    ),
                )
            }
        }
    }
}
