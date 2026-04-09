package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
internal fun ActiveHandGlow(
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlphaState =
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glowAlpha",
        )
    val glowScaleState =
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.3f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glowScale",
        )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = glowAlphaState.value
                    scaleX = glowScaleState.value
                    scaleY = glowScaleState.value
                }.drawWithCache {
                    val radius = size.maxDimension * 0.5f
                    val center = Offset(size.width / 2, size.height / 2)
                    val brush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = radius,
                        )
                    onDrawBehind {
                        drawCircle(brush = brush, center = center, radius = radius)
                    }
                }
    )
}
