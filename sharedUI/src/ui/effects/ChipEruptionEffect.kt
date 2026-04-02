package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.effects.ChipVisuals.drawParticleChip
import kotlin.random.Random

private class ChipParticle(
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
 * Chips arc from a source (starting hand or side bet slot) toward the balance meter.
 *
 * The animation loop suspends automatically while [isPaused] returns `true`,
 * preventing GPU work when the app is backgrounded. Particle state is preserved
 * mid-flight so the effect resumes correctly on foreground.
 *
 * @param isPaused Lambda returning `true` when the host lifecycle is below RESUMED.
 *   Wire this to [BlackjackAnimationState.isPaused] at the call site.
 */
@Composable
fun ChipEruptionEffect(
    modifier: Modifier = Modifier,
    amount: Int,
    startOffset: Offset? = null,
    isPaused: () -> Boolean = { false },
) {
    val chips =
        remember(amount) {
            val calculatedChips = ChipVisuals.breakdownAmountValues(amount)
            calculatedChips
                .mapIndexed { i, chipAmount ->
                    ChipParticle(
                        amount = chipAmount,
                        controlXFraction = Random.nextFloat() * 0.6f + 0.2f,
                        controlYFraction = Random.nextFloat() * 0.25f,
                        speed = Random.nextFloat() * 0.008f + 0.010f,
                        launchDelayFrames = i * 3,
                    )
                }.toMutableList()
        }

    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(chips) {
        runParticleLoop(
            particles = chips,
            frameState = frameState,
            isPaused = isPaused,
            update = { p, _ -> p.update() },
            isDone = { it.isDone },
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue // draw-only invalidation, not recomposition
        val chipRadius = 24.dp.toPx()
        val srcX = startOffset?.x ?: (size.width * 0.5f)
        val srcY = startOffset?.y ?: (size.height * 0.4f)
        val dstY = size.height * 1.1f

        for (i in 0 until chips.size) {
            val chip = chips[i]
            if (chip.t <= 0f) continue

            val t = chip.t
            val cx = chip.controlXFraction * size.width
            val cy = chip.controlYFraction * size.height

            // Spread the destination X based on control X so they fan out
            val dstX = size.width * 0.5f + (chip.controlXFraction - 0.5f) * size.width * 2f

            val inv = 1f - t
            val x = inv * inv * srcX + 2f * inv * t * cx + t * t * dstX
            val y = inv * inv * srcY + 2f * inv * t * cy + t * t * dstY

            val alpha = if (t > 0.8f) 1f - (t - 0.8f) / 0.2f else 1f
            val scale = 1.2f - 0.5f * t

            drawParticleChip(
                color = ChipUtils.chipColor(chip.amount),
                alpha = alpha.coerceIn(0f, 1f),
                radius = chipRadius * scale,
                center = Offset(x, y),
            )
        }
    }
}
