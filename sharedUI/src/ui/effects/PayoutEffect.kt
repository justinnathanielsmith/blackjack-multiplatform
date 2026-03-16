package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.smithjustinn.blackjack.ui.effects.ChipVisuals.drawChipVisual
import kotlin.math.PI
import kotlin.random.Random

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
    val chips = remember(amount) {
        ChipVisuals.breakdownAmount(amount, maxParticles = 8).reversed()
    }
    
    // Stable random offsets for the stack
    val stackVisualOffsets = remember(chips.size) {
        List(chips.size) { index ->
            val angle = (index * 137.5f) * (PI / 180f).toFloat()
            val jitter = 1.0f + (Random.nextFloat() * 1.5f)
            Offset(
                x = (Math.cos(angle.toDouble()).toFloat() * jitter),
                y = -index * 4.5f
            )
        }
    }

    var progress by remember { mutableStateOf(0f) }
    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        val duration = 600L // 600ms for the slide
        val startTime = withFrameNanos { it }
        
        while (progress < 1f) {
            withFrameNanos { time ->
                frameState.longValue = time
                val elapsed = (time - startTime) / 1_000_000L
                progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            }
        }
        onAnimationEnd()
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue // Recompose trigger
        
        val width = size.width
        val height = size.height
        
        // Start from house (top centerish)
        val startX = width * 0.5f 
        val startY = height * 0.15f
        
        // Quad ease in out for the glide
        val t = if (progress < 0.5f) 2f * progress * progress else 1f - Math.pow(-2f * progress + 2.0, 2.0).toFloat() / 2f
        
        val currentX = startX + (targetOffset.x - startX) * t
        val currentY = startY + (targetOffset.y - startY) * t
        
        // Fade in at start, fade out at very end
        val alpha = when {
            progress < 0.1f -> progress / 0.1f
            progress > 0.9f -> 1f - (progress - 0.9f) / 0.1f
            else -> 1f
        }
        
        // Scaling effect (slight pop)
        val scale = if (progress < 0.5f) 1.0f + (progress * 0.4f) else 1.2f - ((progress - 0.5f) * 0.4f)

        withTransform({
            translate(currentX, currentY)
            scale(scale, scale, Offset.Zero)
        }) {
            chips.forEachIndexed { index, color ->
                val offset = stackVisualOffsets[index]
                drawChipVisual(
                    color = color,
                    alpha = alpha,
                    radius = 24f, // Standard radius for effects
                    center = offset
                )
            }
        }
    }
}
