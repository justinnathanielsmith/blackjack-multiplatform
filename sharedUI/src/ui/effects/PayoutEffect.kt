package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.ChipStack

/**
 * An animation where a stack of chips slides from the house/dealer to a specific player hand.
 */
@Composable
fun PayoutEffect(
    amount: Int,
    targetOffset: Offset,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val duration = 600L // 600ms for the slide
        val startTime = withFrameNanos { it }

        while (progress < 1f) {
            withFrameNanos { time ->
                val elapsed = (time - startTime) / 1_000_000L
                progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            }
        }
        onAnimationEnd()
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val chipHalfPx = with(density) { 24.dp.toPx() }
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Start from house (top centerish)
        val startX = width * 0.5f
        val startY = height * 0.15f

        // Quad ease in out for the glide
        val t =
            if (progress < 0.5f) {
                2f * progress * progress
            } else {
                val inv = -2f * progress + 2f
                1f - (inv * inv) / 2f
            }

        val currentX = startX + (targetOffset.x - startX) * t
        val currentY = startY + (targetOffset.y - startY) * t

        // Fade in at start, fade out at very end
        val alpha =
            when {
                progress < 0.1f -> progress / 0.1f
                progress > 0.9f -> 1f - (progress - 0.9f) / 0.1f
                else -> 1f
            }

        // Scaling effect (slight pop)
        val scale = if (progress < 0.5f) 1.0f + (progress * 0.4f) else 1.2f - ((progress - 0.5f) * 0.4f)

        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        translationX = currentX - chipHalfPx
                        translationY = currentY - chipHalfPx
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
        ) {
            ChipStack(amount = amount)
        }
    }
}
