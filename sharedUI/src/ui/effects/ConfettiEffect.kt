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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ParticleShape { RECTANGLE, CIRCLE, STAR, STREAMER }

/**
 * A particle representing a single piece of confetti.
 */
private class Particle(
    val color: Color,
    val size: Float,
    angle: Double,
    speed: Float,
    val shape: ParticleShape,
    val alphaDecay: Float = ALPHA_DECAY,
    val startXFraction: Float? = null,
) {
    var x = 0f
    var y = 0f
    private var vx = cos(angle).toFloat() * speed
    private var vy = sin(angle).toFloat() * speed
    var alpha = 1f
    var rotation = Random.nextFloat() * MAX_ROTATION_DEG
    private val vRot = (Random.nextFloat() - ROTATION_SPEED_OFFSET) * ROTATION_SPEED_MULTIPLIER

    // Bolt Performance Optimization: Pre-allocated Path for STAR shape particles.
    // Reused each frame via fillStarPath() instead of allocating a new Path per draw call.
    // At 60fps with ~10% STAR particles out of 290 max, this eliminates ~1,740 allocations/second.
    val path: Path? = if (shape == ParticleShape.STAR) Path() else null

    fun update() {
        x += vx
        y += vy
        vy += GRAVITY_ACCEL
        alpha -= alphaDecay
        rotation += vRot
    }
}

/**
 * A composable that displays a confetti burst effect.
 *
 * @param modifier The modifier to be applied to the layout.
 * @param particleCount The number of particles to generate.
 * @param isBlackjack Whether this is a blackjack win (uses gold colors and wave 2).
 */
@Composable
fun ConfettiEffect(
    modifier: Modifier = Modifier,
    particleCount: Int = 100,
    isBlackjack: Boolean = false,
) {
    val colors =
        if (isBlackjack) {
            listOf(PrimaryGold, Color(0xFFFFF8DC), Color(0xFFFFD700), Color.White)
        } else {
            listOf(PokerRed, PrimaryGold, PokerBlack, Color.White)
        }

    val particles = remember { ArrayList<Particle>(particleCount + 40) }
    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        // Wave 1: burst from center
        repeat(particleCount) {
            particles.add(
                Particle(
                    color = colors.random(),
                    size = Random.nextFloat() * MAX_PARTICLE_SIZE_DIFF + MIN_PARTICLE_SIZE,
                    angle = Random.nextDouble(0.0, 2.0 * PI),
                    speed = Random.nextFloat() * MAX_PARTICLE_SPEED_DIFF + MIN_PARTICLE_SPEED,
                    shape = randomShape(),
                )
            )
        }

        // Wave 2 (blackjack only): shower from top edge at 400ms
        if (isBlackjack) {
            delay(400)
            repeat(40) {
                particles.add(
                    Particle(
                        color = colors.random(),
                        size = Random.nextFloat() * MAX_PARTICLE_SIZE_DIFF + MIN_PARTICLE_SIZE,
                        angle = PI / 2.0 + (Random.nextDouble() - 0.5) * 0.5,
                        speed = Random.nextFloat() * 4f + 3f,
                        shape = randomShape(),
                        alphaDecay = 0.007f,
                        startXFraction = Random.nextFloat(),
                    )
                )
            }
        }

        // Animation loop
        while (particles.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time

                // Performance Optimization: Update in O(N) then use removeAll which maps to
                // an efficient single-pass O(N) removal, rather than O(N^2) backward while loops with removeAt.
                for (i in 0 until particles.size) {
                    particles[i].update()
                }
                particles.removeAll { it.alpha <= 0f }
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
            val originX =
                if (p.startXFraction != null) {
                    canvasCenter.x + (p.startXFraction - 0.5f) * size.width
                } else {
                    canvasCenter.x
                }
            val position = Offset(originX + p.x, canvasCenter.y + p.y)
            val paintColor = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f))

            rotate(p.rotation, pivot = position) {
                when (p.shape) {
                    ParticleShape.RECTANGLE ->
                        drawRect(
                            color = paintColor,
                            topLeft = position,
                            size = Size(p.size, p.size),
                        )
                    ParticleShape.CIRCLE ->
                        drawCircle(
                            color = paintColor,
                            radius = p.size / 2f,
                            center = position,
                        )
                    ParticleShape.STREAMER ->
                        drawRect(
                            color = paintColor,
                            topLeft = position,
                            size = Size(p.size * 3f, p.size * 0.4f),
                        )
                    ParticleShape.STAR -> {
                        // Reuse the pre-allocated path — reset() clears geometry without heap allocation.
                        val starPath = p.path!!
                        fillStarPath(starPath, position, p.size * 0.5f, p.size * 0.22f)
                        drawPath(path = starPath, color = paintColor)
                    }
                }
            }
        }
    }
}

private fun randomShape(): ParticleShape {
    return when (Random.nextInt(10)) {
        in 0..3 -> ParticleShape.RECTANGLE // 40%
        in 4..6 -> ParticleShape.CIRCLE // 30%
        in 7..8 -> ParticleShape.STREAMER // 20%
        else -> ParticleShape.STAR // 10%
    }
}

/**
 * Fills [path] with 5-pointed star geometry in-place, resetting any previous content.
 * The caller must supply a pre-allocated [Path] to avoid heap allocation on every frame.
 */
private fun fillStarPath(
    path: Path,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float
) {
    path.reset()
    val points = 5
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * PI / points) - PI / 2.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
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
