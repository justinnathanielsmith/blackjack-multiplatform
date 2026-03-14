package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.ModernGold

@Composable
fun SplitHandIndicator(
    isActive: Boolean,
    isResolved: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200),
                repeatMode = RepeatMode.Reverse
            ),
        label = "glowAlpha"
    )

    val containerAlpha = if (isResolved && !isActive) 0.5f else 1f

    Box(
        modifier =
            modifier
                .alpha(containerAlpha)
                .then(
                    if (isActive) {
                        Modifier
                            .shadow(
                                elevation = 16.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = ModernGold.copy(alpha = glowAlpha),
                                ambientColor = ModernGold.copy(alpha = glowAlpha * 0.4f)
                            ).border(
                                width = 2.dp,
                                brush = Brush.radialGradient(
                                    colors = listOf(ModernGold, ModernGold.copy(alpha = 0.3f)),
                                    radius = 1000f
                                ),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .padding(8.dp)
                    } else {
                        Modifier.padding(8.dp)
                    }
                )
    ) {
        content()
    }
}
