package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.BetChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class FlyingChip(
    val id: Long,
    val startOffset: Offset,
    val amount: Int,
    val color: Color,
    val textColor: Color,
)

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
            scaleX.animateTo(1.2f, animationSpec = tween(50))
            scaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }
        launch {
            scaleY.animateTo(0.8f, animationSpec = tween(50))
            scaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }

        delay(120)
        alpha.animateTo(0f, animationSpec = tween(180))
        onAnimationEnd()
    }

    Box(
        modifier =
            Modifier
                .offset { IntOffset(animX.value.toInt(), animY.value.toInt()) }
                .graphicsLayer {
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
