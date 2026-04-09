package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import io.github.smithjustinn.blackjack.ui.components.chips.ChipStack
import kotlinx.coroutines.yield

/**
 * An animation where a stack of chips slides from the house/dealer to a specific player hand.
 *
 * The frame loop suspends automatically while [isPaused] returns `true`, preventing
 * GPU work when the app is backgrounded. Progress state is preserved so the slide
 * resumes from the correct position on foreground.
 *
 * @param isPaused Lambda returning `true` when the host lifecycle is below RESUMED.
 *   Wire this to [BlackjackAnimationState.isPaused] at the call site.
 */
@Composable
fun PayoutEffect(
    amount: Int,
    targetOffset: Offset,
    onAnimationEnd: () -> Unit,
    modifier: Modifier = Modifier,
    isPaused: () -> Boolean = { false },
) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val duration = 600L // 600ms for the slide
        val startTime = withFrameNanos { it }

        while (progress < 1f) {
            // Suspend cheaply while backgrounded — preserves progress mid-slide.
            while (isPaused()) {
                yield()
            }

            withFrameNanos { time ->
                val elapsed = (time - startTime) / 1_000_000L
                progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            }
        }
        onAnimationEnd()
    }

    val density = LocalDensity.current
    val chipHalfPx = with(density) { 24.dp.toPx() }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = progress
                    val width = size.width
                    val height = size.height

                    // Start from house (top centerish)
                    val startX = width * 0.5f
                    val startY = height * 0.15f

                    // Quad ease in out for the glide
                    val t =
                        if (p < 0.5f) {
                            2f * p * p
                        } else {
                            val inv = -2f * p + 2f
                            1f - (inv * inv) / 2f
                        }

                    val currentX = startX + (targetOffset.x - startX) * t
                    val currentY = startY + (targetOffset.y - startY) * t

                    // Fade in at start, fade out at very end
                    val alpha =
                        when {
                            p < 0.1f -> p / 0.1f
                            p > 0.9f -> 1f - (p - 0.9f) / 0.1f
                            else -> 1f
                        }

                    // Scaling effect (slight pop)
                    val scale = if (p < 0.5f) 1.0f + (p * 0.4f) else 1.2f - ((p - 0.5f) * 0.4f)

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
