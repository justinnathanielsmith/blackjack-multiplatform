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
import androidx.compose.ui.graphics.drawscope.withTransform
import io.github.smithjustinn.blackjack.ui.effects.ChipVisuals.drawChipVisual
import kotlin.random.Random

private class ChipLossParticle(
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
 * Chips fly away from the pot towards the dealer on player loss.
 */
@Composable
fun ChipLossEffect(
    modifier: Modifier = Modifier,
    amount: Int,
) {
    val chips =
        remember(amount) {
            val calculatedChips = ChipVisuals.breakdownAmount(amount, maxParticles = 20)
            calculatedChips
                .mapIndexed { i, color ->
                    ChipLossParticle(
                        color = color,
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
                var i = 0
                while (i < chips.size) {
                    val chip = chips[i]
                    chip.update()
                    if (chip.isDone) {
                        chips.removeAt(i)
                    } else {
                        i++
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue

        val width = size.width
        val height = size.height

        val srcX = width * 0.5f // Pot area
        val srcY = height * 0.65f
        val dstY = -height * 0.1f // Fly off-screen top

        for (i in 0 until chips.size) {
            val chip = chips[i]
            if (chip.t <= 0f) continue

            val t = chip.t
            val cx = chip.controlXFraction * width
            val cy = chip.controlYFraction * height

            val dstX = width * 0.5f + (chip.controlXFraction - 0.5f) * width * 1.5f // Dealer area, spread out

            val inv = 1f - t
            val x = inv * inv * srcX + 2f * inv * t * cx + t * t * dstX
            val y = inv * inv * srcY + 2f * inv * t * cy + t * t * dstY

            val alpha = if (t > 0.7f) 1f - (t - 0.7f) / 0.3f else 1f
            val scale = 1.0f + 0.5f * t

            withTransform({
                translate(x, y)
                scale(scale, scale, Offset.Zero)
            }) {
                drawChipVisual(
                    color = chip.color,
                    alpha = alpha.coerceIn(0f, 1f),
                    radius = ChipVisuals.STANDARD_RADIUS,
                    center = Offset.Zero
                )
            }
        }
    }
}
