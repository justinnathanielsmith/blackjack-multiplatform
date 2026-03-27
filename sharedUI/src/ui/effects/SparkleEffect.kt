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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val SPARKLE_COLORS = listOf(PrimaryGold, Color.White, Color(0xFFFFFACD), Color(0xFFFFD700))

private class SparkleParticle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color,
) {
    var x = 0f
    var y = 0f
    var frame = 0
    var alpha = 0f

    // Bolt Performance Optimization: Pre-allocated Path reused each frame instead of
    // allocating a new Path object per draw call. Eliminates ~1,200 Path allocations/second
    // (20 particles × 60fps) during the blackjack-win sparkle effect.
    val path = Path()

    companion object {
        const val RISE_FRAMES = 9
        const val FALL_FRAMES = 24
        const val TOTAL_FRAMES = RISE_FRAMES + FALL_FRAMES
    }

    fun update() {
        frame++
        x += cos(angle) * speed
        y += sin(angle) * speed
        alpha =
            if (frame <= RISE_FRAMES) {
                frame.toFloat() / RISE_FRAMES
            } else {
                1f - (frame - RISE_FRAMES).toFloat() / FALL_FRAMES
            }
    }

    val isDone: Boolean get() = frame >= TOTAL_FRAMES
}

/**
 * 4-pointed star burst effect for blackjack wins.
 * 20 particles evenly spaced, travel outward and fade over ~550ms.
 */
@Composable
fun SparkleEffect(modifier: Modifier = Modifier) {
    val particleCount = 20
    val particles =
        remember {
            ArrayList<SparkleParticle>(particleCount).also { list ->
                repeat(particleCount) { i ->
                    val angle = (i.toFloat() / particleCount * 2f * PI).toFloat()
                    list.add(
                        SparkleParticle(
                            angle = angle,
                            speed = Random.nextFloat() * 4f + 3.5f,
                            size = Random.nextFloat() * 12f + 16f,
                            color = SPARKLE_COLORS.random(),
                        )
                    )
                }
            }
        }

    val frameState = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (particles.isNotEmpty()) {
            withFrameNanos { time ->
                frameState.longValue = time
                // Performance Optimization: Update in O(N) then use removeAll which maps to
                // an efficient single-pass O(N) removal, rather than O(N^2) backward while loops with removeAt.
                for (i in 0 until particles.size) {
                    particles[i].update()
                }
                particles.removeAll { it.isDone }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        frameState.longValue
        val origin = center

        for (i in 0 until particles.size) {
            val p = particles[i]
            val pos = Offset(origin.x + p.x, origin.y + p.y)
            val paintColor = p.color.copy(alpha = p.alpha.coerceIn(0f, 1f))
            // Reuse the pre-allocated path — reset() clears geometry without heap allocation.
            fillSparklePath(p.path, pos, p.size, p.size * 0.2f)
            rotate(p.frame * 3f, pivot = pos) {
                drawPath(
                    path = p.path,
                    color = paintColor,
                )
            }
        }
    }
}

/**
 * Fills [path] with 4-pointed star geometry in-place, resetting any previous content.
 * The caller must supply a pre-allocated [Path] to avoid heap allocation on every frame.
 */
private fun fillSparklePath(
    path: Path,
    center: Offset,
    outerRadius: Float,
    innerRadius: Float
) {
    path.reset()
    val points = 4
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * PI / points) - PI / 2.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
}
