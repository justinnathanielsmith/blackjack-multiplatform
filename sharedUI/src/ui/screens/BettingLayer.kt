package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationState
import io.github.smithjustinn.blackjack.ui.effects.ChipEruptionEffect
import io.github.smithjustinn.blackjack.ui.effects.ChipLossEffect
import io.github.smithjustinn.blackjack.ui.effects.PayoutEffect
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants

@Composable
fun BettingLayer(
    status: GameStatus,
    handCount: Int,
    sideBets: Map<SideBetType, Int>,
    playerHands: List<Hand>,
    animState: BlackjackAnimationState,
    component: BlackjackComponent,
    selectedAmount: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = status == GameStatus.BETTING,
        modifier = modifier.zIndex(5f),
        enter =
            slideInVertically(
                initialOffsetY = { it / 2 }
            ) + fadeIn(tween(AnimationConstants.BettingPhaseEnterDuration)),
        exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
    ) {
        BettingPhaseScreen(
            handCount = handCount,
            sideBets = sideBets,
            playerHands = playerHands,
            component = component,
            selectedAmount = selectedAmount,
            modifier = modifier,
        )
    }

    for (i in 0 until animState.chipEruptions.size) {
        val instance = animState.chipEruptions[i]
        key(instance.id) {
            ChipEruptionEffect(
                amount = instance.amount,
                startOffset = instance.startOffset,
                isPaused = { animState.isPaused },
            )
        }
    }
    for (i in 0 until animState.chipLosses.size) {
        val instance = animState.chipLosses[i]
        key(instance.id) {
            ChipLossEffect(
                amount = instance.amount,
                isPaused = { animState.isPaused },
            )
        }
    }

    for (i in 0 until animState.activePayouts.size) {
        val instance = animState.activePayouts[i]
        key(instance.id) {
            PayoutEffect(
                amount = instance.amount,
                targetOffset = instance.targetOffset,
                onAnimationEnd = { animState.activePayouts.remove(instance) },
                isPaused = { animState.isPaused },
            )
        }
    }
}
