package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.smithjustinn.blackjack.ui.theme.ChipBlue
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.ChipPurple
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft
import kotlin.math.PI

object ChipVisuals {
    const val STANDARD_RADIUS = 24f
    private val DASH_LENGTH = (STANDARD_RADIUS * 2 * PI / 12).toFloat()
    private val STANDARD_STROKE =
        Stroke(
            width = STANDARD_RADIUS * 0.16f,
            pathEffect =
                PathEffect.dashPathEffect(
                    floatArrayOf(DASH_LENGTH / 2, DASH_LENGTH / 2),
                    0f
                )
        )

    fun breakdownAmount(
        amount: Int,
        maxParticles: Int = 30
    ): List<Color> {
        val result = mutableListOf<Color>()
        var remaining = amount

        val denominations =
            listOf(
                500 to ChipPurple,
                100 to PokerBlack,
                25 to ChipGreen,
                10 to ChipBlue,
                5 to PokerRed,
                1 to WhiteSoft
            )

        for ((value, color) in denominations) {
            val count = remaining / value
            if (count > 0) {
                val toAdd = minOf(count, maxParticles - result.size)
                repeat(toAdd) {
                    result.add(color)
                }
                remaining -= count * value
            }
            if (result.size >= maxParticles) {
                break
            }
        }

        if (remaining > 0 && result.size < maxParticles) {
            result.add(WhiteSoft)
        }

        return result.shuffled()
    }

    fun DrawScope.drawChipVisual(
        color: Color,
        alpha: Float,
        radius: Float,
        center: Offset
    ) {
        val mainColor = color.copy(alpha = alpha)
        val accentColor = Color.White.copy(alpha = alpha * 0.6f)

        // Subtle depth effect
        val depthOffset = radius * 0.08f
        drawCircle(
            color = Color.Black.copy(alpha = alpha * 0.3f),
            radius = radius,
            center = center + Offset(0f, depthOffset)
        )

        // Main circle
        drawCircle(
            color = mainColor,
            radius = radius,
            center = center
        )

        // Classic casino dashed rim
        val stroke =
            if (radius == STANDARD_RADIUS) {
                STANDARD_STROKE
            } else {
                val dashLength = (radius * 2 * PI / 12).toFloat()
                Stroke(
                    width = radius * 0.16f,
                    pathEffect =
                        PathEffect.dashPathEffect(
                            floatArrayOf(dashLength / 2, dashLength / 2),
                            0f
                        )
                )
            }

        drawCircle(
            color = accentColor,
            radius = radius * 0.92f,
            center = center,
            style = stroke
        )

        // Center inlay
        drawCircle(
            color = Color.White.copy(alpha = alpha * 0.2f),
            radius = radius * 0.6f,
            center = center
        )
    }
}
