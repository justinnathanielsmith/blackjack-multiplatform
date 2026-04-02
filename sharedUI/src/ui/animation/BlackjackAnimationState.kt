package io.github.smithjustinn.blackjack.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

@Stable
class BlackjackAnimationState {
    val shakeOffset = Animatable(0f)
    val flashAlpha = Animatable(0f)
    var flashColor by mutableStateOf(Color.White)
    var nearMissHandIndex by mutableStateOf<Int?>(null)
    val chipEruptions = mutableStateListOf<ChipEruptionInstance>()
    val chipLosses = mutableStateListOf<ChipLossInstance>()
    val activePayouts = mutableStateListOf<PayoutInstance>()

    /**
     * Set to `true` by the platform entry-point when the app moves below the
     * RESUMED lifecycle state (e.g. backgrounded on Android/iOS, minimized on Desktop).
     *
     * All [runParticleLoop] callers read this flag and yield cheaply rather than
     * driving [withFrameNanos] callbacks that would wake the GPU with no visible output.
     * Particle state is preserved mid-flight so effects resume correctly on foreground.
     */
    var isPaused: Boolean by mutableStateOf(false)
}

@Immutable
data class PayoutInstance(
    val id: Long,
    val amount: Int,
    val targetOffset: Offset,
)

@Immutable
data class ChipEruptionInstance(
    val id: Long,
    val amount: Int,
    val startOffset: Offset?,
)

@Immutable
data class ChipLossInstance(
    val id: Long,
    val amount: Int,
)
