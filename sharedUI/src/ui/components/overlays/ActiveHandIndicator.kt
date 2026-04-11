package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
internal fun ActiveHandIndicator(
    transition: InfiniteTransition,
    modifier: Modifier = Modifier,
) {
    val bounceOffsetState =
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.ActiveHandIndicatorDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "bounceOffset",
        )

    val glowAlphaState =
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "indicatorGlowAlpha",
        )

    Box(
        modifier =
            modifier
                .size(24.dp, 16.dp)
                .graphicsLayer {
                    translationY = bounceOffsetState.value
                }.drawBehind {
                    // Draw a premium metallic chevron / arrow
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = glowAlphaState.value * 0.5f), Color.Transparent),
                            radius = size.maxDimension * 1.5f,
                        )
                    drawCircle(brush = glowBrush, radius = size.maxDimension * 1.5f)

                    val path =
                        androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width / 2, size.height)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2, size.height * 0.4f)
                            close()
                        }

                    // Shadow/Depth
                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.4f),
                    )

                    // Main Body - Metallic Gold
                    val metallicBrush =
                        Brush.verticalGradient(
                            colors = listOf(ModernGoldLight, PrimaryGold, ModernGoldDark)
                        )
                    drawPath(
                        path = path,
                        brush = metallicBrush,
                    )

                    // Rim Highlight
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
    )
}
