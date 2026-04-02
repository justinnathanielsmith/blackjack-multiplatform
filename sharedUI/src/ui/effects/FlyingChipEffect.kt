package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.BetChip
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
    val density = LocalDensity.current
    val dropHeight = with(density) { 64.dp.toPx() }

    val animX = remember { Animatable(chip.startOffset.x) }
    val animY = remember { Animatable(chip.startOffset.y) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val aboveY = targetOffset.y - dropHeight
        launch {
            animX.animateTo(
                targetValue = targetOffset.x,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
        }
        animY.animateTo(
            targetValue = aboveY,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )

        animY.animateTo(
            targetValue = targetOffset.y,
            animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
        )

        launch {
            scaleX.animateTo(1.2f, animationSpec = tween(AnimationConstants.ChipSquashDuration))
            scaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }
        launch {
            scaleY.animateTo(0.8f, animationSpec = tween(AnimationConstants.ChipSquashDuration))
            scaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
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
                    this.alpha = alpha.value
                    this.scaleX = scaleX.value
                    this.scaleY = scaleY.value
                },
    ) {
        BetChip(
            amount = chip.amount,
            chipColor = chip.color,
            textColor = chip.textColor,
        )
    }
}
