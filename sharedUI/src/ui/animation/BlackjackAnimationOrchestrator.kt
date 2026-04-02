package io.github.smithjustinn.blackjack.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlin.random.Random

object BlackjackAnimationOrchestrator {
    suspend fun orchestrate(
        effects: Flow<GameEffect>,
        stateFlow: StateFlow<GameState>,
        animState: BlackjackAnimationState,
        audioService: AudioService,
        hapticsService: HapticsService,
    ) = coroutineScope {
        // 1. Effects pipeline — audio/haptics dispatching + animation state mutations
        launch {
            effects.collect { effect ->
                handleGameEffect(effect, hapticsService, audioService)
                when (effect) {
                    is GameEffect.NearMissHighlight -> launch {
                        animState.nearMissHandIndex = effect.handIndex
                        delay(AnimationConstants.NearMissLifetimeMs)
                        animState.nearMissHandIndex = null
                    }
                    is GameEffect.ChipEruption -> launch {
                        val instance = ChipEruptionInstance(Random.nextLong(), effect.amount, null)
                        animState.chipEruptions.add(instance)
                        delay(AnimationConstants.ChipEruptionLifetimeMs)
                        animState.chipEruptions.remove(instance)
                    }
                    is GameEffect.ChipLoss -> launch {
                        val instance = ChipLossInstance(Random.nextLong(), effect.amount)
                        animState.chipLosses.add(instance)
                        delay(AnimationConstants.ChipLossLifetimeMs)
                        animState.chipLosses.remove(instance)
                    }
                    else -> {}
                }
            }
        }

        // 2. State-driven flash and shake — cancels the previous animation job on each status change
        launch {
            var flashJob: Job? = null
            var shakeJob: Job? = null
            stateFlow.distinctUntilChangedBy { it.status }.collect { state ->
                when (state.status) {
                    GameStatus.PLAYER_WON -> {
                        flashJob?.cancel()
                        flashJob = launch {
                            val isBlackjack = state.playerHands.any { it.isBlackjack }
                            animState.flashColor = if (isBlackjack) PrimaryGold else Color.White
                            val targetAlpha = if (isBlackjack) 0.25f else 0.15f
                            val outDuration =
                                if (isBlackjack) AnimationConstants.FlashOutDurationBlackjack
                                else AnimationConstants.FlashOutDurationWin
                            animState.flashAlpha.animateTo(targetAlpha, tween(AnimationConstants.FlashInDuration))
                            animState.flashAlpha.animateTo(0f, tween(outDuration))
                        }
                    }
                    GameStatus.DEALER_WON -> {
                        shakeJob?.cancel()
                        shakeJob = launch {
                            animState.shakeOffset.animateTo(
                                AnimationConstants.ShakeDistance,
                                spring<Float>(stiffness = Spring.StiffnessHigh),
                            )
                            animState.shakeOffset.animateTo(
                                -AnimationConstants.ShakeDistance,
                                spring<Float>(stiffness = Spring.StiffnessHigh),
                            )
                            animState.shakeOffset.animateTo(
                                AnimationConstants.ShakeDistanceRebound,
                                spring<Float>(stiffness = Spring.StiffnessHigh),
                            )
                            animState.shakeOffset.animateTo(
                                -AnimationConstants.ShakeDistanceRebound,
                                spring<Float>(stiffness = Spring.StiffnessHigh),
                            )
                            animState.shakeOffset.animateTo(
                                0f,
                                spring<Float>(stiffness = Spring.StiffnessMedium),
                            )
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}
