package com.nerdwatch.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Text

/**
 * A Michroma stencil label: uppercase, letter-spaced, never an icon.
 *
 * The design uses text labels exclusively — no emoji, no iconography — so this
 * is the only way a label should reach the screen.
 */
@Composable
fun StencilText(
    text: String,
    fontSizePx: Float,
    color: Color,
    trackingPx: Float,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Center,
    fontWeight: FontWeight = FontWeight.Normal,
) {
    val density = LocalDensity.current

    Text(
        text = text.uppercase(),
        color = color,
        textAlign = textAlign,
        modifier = modifier,
        maxLines = 1,
        style = TextStyle(
            fontFamily = AvionicsFonts.Stencil,
            fontWeight = fontWeight,
            fontSize = with(density) { fontSizePx.toSp() },
            letterSpacing = with(density) { trackingPx.toSp() },
            // Michroma carries generous ascent/descent. Untrimmed, each label
            // reserves ~30% more height than the spec allots, and the stack of
            // them pushes the next-event chip off the bottom of the face.
            lineHeight = with(density) { (fontSizePx * 1.1f).toSp() },
            platformStyle = PlatformTextStyle(includeFontPadding = false),
            lineHeightStyle = LineHeightStyle(
                alignment = LineHeightStyle.Alignment.Center,
                trim = LineHeightStyle.Trim.Both,
            ),
        ),
    )
}
