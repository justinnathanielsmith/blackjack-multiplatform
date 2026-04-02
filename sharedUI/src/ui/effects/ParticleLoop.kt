package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Drives a frame-by-frame particle simulation loop that **suspends automatically
 * while the app is backgrounded**, preventing unnecessary CPU/GPU consumption.
 *
 * ## Lifecycle integration
 *
 * This project uses Essenty (Decompose) rather than androidx.lifecycle, so
 * lifecycle-aware suspension is accomplished via [isPaused] — a flag stored in
 * [io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationState] and
 * flipped by the platform entry-point (MainActivity / Main.kt / IosEntryPoint)
 * whenever the app moves in or out of the foreground.
 *
 * When [isPaused] returns `true`:
 * - The loop spins on a suspending [kotlinx.coroutines.yield] until the flag
 *   clears, burning zero GPU.
 * - Particle state is preserved exactly so the effect resumes from mid-flight.
 *
 * ## Usage pattern
 *
 * ```kotlin
 * LaunchedEffect(chips) {
 *     runParticleLoop(
 *         particles = chips,
 *         frameState = frameState,
 *         isPaused = { animState.isPaused },
 *         update = { p, _ -> p.update() },
 *         isDone = { it.isDone },
 *     )
 * }
 * ```
 *
 * ## Performance note
 *
 * [update] receives the raw nanosecond timestamp from [withFrameNanos] for
 * physics effects that need frame-delta time. Callers that use frame-count
 * rather than wall-clock time may ignore the parameter.
 *
 * @param P The particle type.
 * @param particles Mutable list of live particles; entries are removed when [isDone] is true.
 * @param frameState A [androidx.compose.runtime.MutableLongState] written each frame to
 *   invalidate only the [androidx.compose.foundation.Canvas] draw scope without recomposing.
 * @param isPaused Returns `true` while the host lifecycle is below RESUMED.
 * @param update Called once per frame per live particle with the current frame time in nanos.
 * @param isDone Returns `true` when a particle has finished its life and should be removed.
 */
suspend fun <P> runParticleLoop(
    particles: MutableList<P>,
    frameState: androidx.compose.runtime.MutableLongState,
    isPaused: () -> Boolean,
    update: (P, Long) -> Unit,
    isDone: (P) -> Boolean,
) {
    while (particles.isNotEmpty() && coroutineContext.isActive) {
        // Suspend cheaply while backgrounded — preserves particle state mid-flight.
        while (isPaused()) {
            kotlinx.coroutines.yield()
        }

        withFrameNanos { time ->
            frameState.longValue = time
            // O(N) update pass
            for (i in 0 until particles.size) {
                update(particles[i], time)
            }
            // O(N) single-pass removal — avoids O(N²) backward removeAt loops
            particles.removeAll { isDone(it) }
        }
    }
}
