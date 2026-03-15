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
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlin.random.Random

private class CoinParticle(
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
 * Coins arc from the hand area toward the balance meter on player win.
 */
@Composable
fun CoinEruptionEffect(
    modifier: Modifier = Modifier,
    coinCount: Int = 10,
) {
    val coins =
        remember {
            ArrayList<CoinParticle>(coinCount).also { list ->
                repeat(coinCount) { i ->
                    list.add(
                        CoinParticle(
                            controlXFraction = Random.nextFloat() * 0.6f + 0.2f,
                            controlYFraction = Random.nextFloat() * 0.25f,
                            speed = Random.nextFloat() * 0.008f + 0.010f,
                            launchDelayFrames = i * 4,
                        )
                    )
                }
            }
        }
    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (coins.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time
                for (i in coins.indices.reversed()) {
                    coins[i].update()
                    if (coins[i].isDone) coins.removeAt(i)
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue

        val width = size.width
        val height = size.height

        val srcX = width * 0.5f
        val srcY = height * 0.65f
        val dstX = width * 0.08f
        val dstY = height * 0.06f

        for (coin in coins) {
            if (coin.t <= 0f) continue

            val t = coin.t
            val cx = coin.controlXFraction * width
            val cy = coin.controlYFraction * height

            // Quadratic Bezier: B(t) = (1-t)²·P0 + 2(1-t)t·P1 + t²·P2
            val inv = 1f - t
            val x = inv * inv * srcX + 2f * inv * t * cx + t * t * dstX
            val y = inv * inv * srcY + 2f * inv * t * cy + t * t * dstY

            val alpha = if (t > 0.8f) 1f - (t - 0.8f) / 0.2f else 1f
            val scale = 1f - 0.7f * t
            val radius = COIN_RADIUS * scale

            drawCircle(
                color = PrimaryGold.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = radius,
                center = Offset(x, y),
            )
            drawCircle(
                color = COIN_INNER_COLOR.copy(alpha = alpha.coerceIn(0f, 1f)),
                radius = radius * COIN_INNER_RADIUS_FRACTION,
                center = Offset(x, y),
            )
        }
    }
}

private const val COIN_RADIUS = 18f
private const val COIN_INNER_RADIUS_FRACTION = 0.55f
private val COIN_INNER_COLOR = Color(0xFFC8A800)
