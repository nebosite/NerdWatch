package com.nerdwatch.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.nerdwatch.design.AvionicsPalette
import com.nerdwatch.design.DesignScale

/**
 * The zero-flash. A full-face accent wash pulsing 0.12 ↔ 0.55, over TIME UP /
 * TAP TO DISMISS. Sits above whatever screen is showing; a tap dismisses it.
 */
@Composable
fun TimeUpOverlay(
    palette: AvionicsPalette,
    scale: DesignScale,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    fun d(px: Float): Dp = with(density) { scale.px(px).toDp() }

    val transition = rememberInfiniteTransition(label = "timeup")
    val alpha by transition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.55f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "timeup-alpha",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(palette.accent).copy(alpha = alpha))
            .longPressGesture(
                longPressEnabled = false,
                scope = scope,
                onTap = onDismiss,
                onLongPress = {},
                onProgress = {},
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        StencilText(
            text = "TIME UP",
            fontSizePx = scale.px(40f),
            color = Color(palette.fg),
            trackingPx = scale.px(4f),
        )
        Spacer(Modifier.height(d(10f)))
        StencilText(
            text = "TAP TO DISMISS",
            fontSizePx = scale.px(12f),
            color = Color(palette.fg),
            trackingPx = scale.px(3f),
        )
    }
}
