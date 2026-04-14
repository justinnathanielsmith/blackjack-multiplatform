package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.ui.components.cards.CardShape
import io.github.smithjustinn.blackjack.ui.components.cards.DealerCard
import io.github.smithjustinn.blackjack.ui.components.cards.PlayingCard
import io.github.smithjustinn.blackjack.ui.components.layout.flightProgress
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun PositionedCardItem(
    card: Card,
    animDelay: Int,
    isFaceUp: Boolean,
    isDealer: Boolean,
    isSlowRoll: Boolean = false,
    baseCardW: Float,
    baseCardH: Float,
    coordOffsetX: Float,
    coordOffsetY: Float,
    isNearMiss: Boolean,
    density: Density,
    isActive: Boolean,
    alpha: Float,
    cardIndexInHand: Int,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    isDoubleDown: Boolean = false,
) {
    val progress = remember { Animatable(0f) }
    var midFlightReached by remember { mutableStateOf(false) }

    val stackBoostPx = with(density) { (cardIndexInHand * 3).dp.toPx() }
    val currentShadow = remember { Animatable(with(density) { 5.dp.toPx() } + stackBoostPx) }
    val animatedScale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(animDelay.toLong())

        val zeta = 0.65f
        val durationSec = AnimationConstants.CardRevealDurationDefault / 1000f
        val stiffness =
            if (durationSec > 0) {
                val omegaN = 6.9f / (zeta * durationSec)
                (omegaN * omegaN).coerceIn(10f, 2000f)
            } else {
                Spring.StiffnessMedium
            }

        launch {
            snapshotFlow { progress.value }
                .first { it >= 0.5f }
            midFlightReached = true
        }

        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness)
            )
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        launch {
            val flyingElevPx = with(density) { 16.dp.toPx() }
            val landedElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
            currentShadow.animateTo(flyingElevPx, tween(AnimationConstants.CardFlightShadowRiseDuration))
            delay(AnimationConstants.CardRevealDurationDefault.toLong() + 150L)
            currentShadow.animateTo(landedElevPx, tween(AnimationConstants.CardShadowLandDuration))
        }
    }

    val targetScale =
        if (isActive && !isDealer) {
            1.1f
        } else if (isActive) {
            1.05f
        } else {
            1f
        }
    LaunchedEffect(targetScale) {
        animatedScale.animateTo(targetScale, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(isActive, cardIndexInHand) {
        val targetElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
        currentShadow.animateTo(targetElevPx, tween(AnimationConstants.CardShadowLandDuration))
    }

    val wasFaceDown = remember { card.isFaceDown }

    Box(
        modifier =
            modifier
                .flightProgress(progress.asState())
                .requiredWidth(Dimensions.Card.StandardWidth)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = animatedScale.value
                    this.scaleY = animatedScale.value
                    shadowElevation = currentShadow.value
                    shape = CardShape
                    clip = false
                }
    ) {
        if (wasFaceDown) {
            DealerCard(
                card = card,
                isFaceUp = isFaceUp,
                isSlowRoll = isSlowRoll,
                scale = 1f,
                shadowElevation = 0.dp,
            )
        } else {
            PlayingCard(
                card = card,
                isFaceUp = isFaceUp && midFlightReached,
                scale = 1f,
                isNearMiss = isNearMiss,
                isDimmed = isDimmed,
                isDoubleDown = isDoubleDown,
                shadowElevation = 0.dp,
                spotColor = if (isActive) PrimaryGold else Color.Black
            )
        }
    }
}
