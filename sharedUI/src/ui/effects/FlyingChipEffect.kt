package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import io.github.smithjustinn.blackjack.ui.components.chips.BetChip
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FlyingChip(
    val id: Long
) {
    var isActive by mutableStateOf(false)
    var startOffset by mutableStateOf(Offset.Zero)
    var targetOffset by mutableStateOf(Offset.Zero)
    var amount by mutableIntStateOf(0)
    var color by mutableStateOf(Color.Unspecified)
    var textColor by mutableStateOf(Color.Unspecified)
}

@Composable
fun FlyingChipAnimation(
    chip: FlyingChip,
    targetOffset: Offset,
    onAnimationEnd: () -> Unit,
) {
    val animX = remember { Animatable(chip.startOffset.x) }
    val animY = remember { Animatable(chip.startOffset.y) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch {
            animX.animateTo(
                targetValue = targetOffset.x,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
        }
        launch {
            animY.animateTo(
                targetValue = targetOffset.y,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
        }
        launch {
            // Subtle rotation during the slide to convey momentum
            rotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            )
        }
        launch {
            // Slight scale bump as it "lifts" off the rack and settles on the table
            scale.animateTo(1.1f, animationSpec = tween(durationMillis = 125, easing = FastOutSlowInEasing))
            scale.animateTo(1.0f, animationSpec = tween(durationMillis = 125, easing = FastOutSlowInEasing))
        }

        delay(AnimationConstants.ChipPreFadeDelayMs)
        alpha.animateTo(0f, animationSpec = tween(AnimationConstants.ChipFadeDuration))
        onAnimationEnd()
    }

    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    this.translationX = animX.value
                    this.translationY = animY.value
                    this.rotationZ = rotation.value
                    this.alpha = alpha.value
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                },
    ) {
        BetChip(
            amount = chip.amount,
            chipColor = chip.color,
            textColor = chip.textColor,
        )
    }
}
