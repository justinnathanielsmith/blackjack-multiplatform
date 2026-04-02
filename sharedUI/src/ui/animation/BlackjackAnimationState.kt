package io.github.smithjustinn.blackjack.ui.animation

import androidx.compose.animation.core.Animatable
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
}

data class PayoutInstance(
    val id: Long,
    val amount: Int,
    val targetOffset: Offset,
)

data class ChipEruptionInstance(
    val id: Long,
    val amount: Int,
    val startOffset: Offset?,
)

data class ChipLossInstance(
    val id: Long,
    val amount: Int,
)
