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
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.effects.ChipVisuals.drawParticleChip
import kotlin.random.Random

private class ChipLossParticle(
    val amount: Int,
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
 * Chips fly away from the pot towards the dealer on player loss.
 */
@Composable
fun ChipLossEffect(
    modifier: Modifier = Modifier,
    amount: Int,
) {
    val chips =
        remember(amount) {
            val calculatedChips = ChipVisuals.breakdownAmountValues(amount, maxParticles = 20)
            calculatedChips
                .mapIndexed { i, chipAmount ->
                    ChipLossParticle(
                        amount = chipAmount,
                        controlXFraction = Random.nextFloat() * 0.4f + 0.3f,
                        controlYFraction = Random.nextFloat() * 0.2f + 0.1f,
                        speed = Random.nextFloat() * 0.010f + 0.012f,
                        launchDelayFrames = i * 2,
                    )
                }.toMutableList()
        }

    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(chips) {
        while (chips.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time
                // Performance Optimization: Update in O(N) then use removeAll which maps to
                // an efficient single-pass O(N) removal, rather than O(N^2) backward while loops with removeAt.
                for (i in 0 until chips.size) {
                    chips[i].update()
                }
                chips.removeAll { it.isDone }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue // draw-only invalidation, not recomposition
        val chipRadius = 24.dp.toPx()
        val srcX = size.width * 0.5f // Pot area
        val srcY = size.height * 0.65f
        val dstY = -size.height * 0.1f // Fly off-screen top

        for (i in 0 until chips.size) {
            val chip = chips[i]
            if (chip.t <= 0f) continue

            val t = chip.t
            val cx = chip.controlXFraction * size.width
            val cy = chip.controlYFraction * size.height

            val dstX = size.width * 0.5f + (chip.controlXFraction - 0.5f) * size.width * 1.5f // Dealer area, spread out

            val inv = 1f - t
            val x = inv * inv * srcX + 2f * inv * t * cx + t * t * dstX
            val y = inv * inv * srcY + 2f * inv * t * cy + t * t * dstY

            val alpha = if (t > 0.7f) 1f - (t - 0.7f) / 0.3f else 1f
            val scale = 1.0f + 0.5f * t

            drawParticleChip(
                color = ChipUtils.chipColor(chip.amount),
                alpha = alpha.coerceIn(0f, 1f),
                radius = chipRadius * scale,
                center = Offset(x, y),
            )
        }
    }
}
