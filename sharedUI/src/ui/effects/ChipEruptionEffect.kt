package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.smithjustinn.blackjack.ui.effects.ChipVisuals.drawChipVisual
import kotlin.random.Random

private class ChipParticle(
    val color: Color,
    val controlXFraction: Float,
    val controlYFraction: Float,
    val speed: Float,
    val launchDelayFrames: Int,
) {
    var t = 0f
    var frameCount = 0

    fun update() {
        frameCount++
        if (frameCount <= launchDelayFrames) return
        t = (t + speed).coerceAtMost(1f)
    }

    val isDone: Boolean get() = t >= 1f
}

/**
 * Chips arc from a source (starting hand or side bet slot) toward the balance meter.
 */
@Composable
fun ChipEruptionEffect(
    modifier: Modifier = Modifier,
    amount: Int,
    startOffset: Offset? = null,
) {
    val chips =
        remember(amount) {
            val calculatedChips = ChipVisuals.breakdownAmount(amount)
            calculatedChips
                .mapIndexed { i, color ->
                    ChipParticle(
                        color = color,
                        controlXFraction = Random.nextFloat() * 0.6f + 0.2f,
                        controlYFraction = Random.nextFloat() * 0.25f,
                        speed = Random.nextFloat() * 0.008f + 0.010f,
                        launchDelayFrames = i * 3,
                    )
                }.toMutableList()
        }

    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(chips) {
        while (chips.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time
                val iterator = chips.iterator()
                while (iterator.hasNext()) {
                    val chip = iterator.next()
                    chip.update()
                    if (chip.isDone) iterator.remove()
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue

        val width = size.width
        val height = size.height

        val srcX = startOffset?.x ?: (width * 0.5f)
        val srcY = startOffset?.y ?: (height * 0.4f)
        val dstY = height * 1.1f

        for (chip in chips) {
            if (chip.t <= 0f) continue

            val t = chip.t
            val cx = chip.controlXFraction * width
            val cy = chip.controlYFraction * height

            // Spread the destination X based on control X so they fan out
            val dstX = width * 0.5f + (chip.controlXFraction - 0.5f) * width * 2f

            val inv = 1f - t
            val x = inv * inv * srcX + 2f * inv * t * cx + t * t * dstX
            val y = inv * inv * srcY + 2f * inv * t * cy + t * t * dstY

            val alpha = if (t > 0.8f) 1f - (t - 0.8f) / 0.2f else 1f
            val scale = 1.2f - 0.5f * t
            val radius = CHIP_RADIUS * scale

            drawChipVisual(
                color = chip.color,
                alpha = alpha.coerceIn(0f, 1f),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

private const val CHIP_RADIUS = 24f
