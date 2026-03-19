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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A particle representing a single piece of confetti.
 */
private class Particle(
    val color: Color,
    val size: Float,
    angle: Double,
    speed: Float,
) {
    var x = 0f
    var y = 0f
    private var vx = cos(angle).toFloat() * speed
    private var vy = sin(angle).toFloat() * speed
    var alpha = 1f
    var rotation = Random.nextFloat() * MAX_ROTATION_DEG
    private val vRot = (Random.nextFloat() - ROTATION_SPEED_OFFSET) * ROTATION_SPEED_MULTIPLIER

    fun update() {
        x += vx
        y += vy
        vy += GRAVITY_ACCEL
        alpha -= ALPHA_DECAY
        rotation += vRot
    }
}

/**
 * A composable that displays a confetti burst effect.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param particleCount The number of particles to generate.
 * @param colors The list of colors to use for the particles.
 */
@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    colors: List<Color> =
        listOf(
            PokerRed,
            PrimaryGold,
            PokerBlack,
            Color.White,
        ),
) {
    val particles = remember { ArrayList<Particle>(particleCount) }
    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        // Create a burst of particles directly in the list to avoid intermediate List allocation
        repeat(particleCount) {
            particles.add(
                Particle(
                    color = colors.random(),
                    size = Random.nextFloat() * MAX_PARTICLE_SIZE_DIFF + MIN_PARTICLE_SIZE,
                    angle = Random.nextDouble(0.0, 2.0 * PI),
                    speed = Random.nextFloat() * MAX_PARTICLE_SPEED_DIFF + MIN_PARTICLE_SPEED,
                )
            )
        }

        // Animation loop
        while (particles.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time

                var i = 0
                while (i < particles.size) {
                    val p = particles[i]
                    p.update()
                    if (p.alpha <= 0) {
                        particles.removeAt(i)
                    } else {
                        i++
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Reading frameState here triggers redraw of the Canvas on every frame
        // without recomposing the entire ConfettiEffect composable.
        frameState.longValue

        val canvasCenter = center
        for (i in 0 until particles.size) {
            val p = particles[i]
            val position = canvasCenter + Offset(p.x, p.y)
            rotate(p.rotation, pivot = position) {
                drawRect(
                    color = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f)),
                    topLeft = position,
                    size = Size(p.size, p.size),
                )
            }
        }
    }
}

private const val MAX_ROTATION_DEG = 360f
private const val ROTATION_SPEED_MULTIPLIER = 20f
private const val ROTATION_SPEED_OFFSET = 0.5f
private const val GRAVITY_ACCEL = 0.2f
private const val ALPHA_DECAY = 0.01f
private const val MAX_PARTICLE_SIZE_DIFF = 10f
private const val MIN_PARTICLE_SIZE = 8f
private const val MAX_PARTICLE_SPEED_DIFF = 12f
private const val MIN_PARTICLE_SPEED = 4f
