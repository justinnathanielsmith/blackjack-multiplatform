package io.github.smithjustinn.blackjack.ui.animation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.handResult
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * The central coordinator for Blackjack UI sensory feedback, including animations, audio, and haptics.
 *
 * This orchestrator bridges the gap between the domain model (represented by [GameState] and [GameEffect] flows)
 * and the platform-specific presentation layers. It ensures that game logic transitions are
 * reflected visually and tactilely with specific timing and priority.
 *
 * The orchestration logic is divided into two primary pipelines:
 * 1. **Effect-driven**: Responds to discrete [GameEffect]s emitted by the state machine (e.g., card plinks, chips).
 * 2. **State-driven**: Reacts to transitions in [GameStatus] (e.g., flashing on a win, shaking on a loss).
 */
object BlackjackAnimationOrchestrator {
    /**
     * Initiates the twin orchestration pipelines within the provided [coroutineScope].
     *
     * This function suspends until the [effects] and [stateFlow] terminators are reached or the scope
     * is cancelled. It maintains shared state for animation jobs (like screen flashes and shakes) to
     * ensure that new transitions correctly preempt or complement in-progress animations.
     *
     * @param effects A [Flow] of [GameEffect]s dispatched by the state machine's reduction loop.
     * @param stateFlow A [StateFlow] providing the single source of truth for the game's current state.
     * @param animState A mutable holder for animation-related observables (offsets, alphas, eruption lists).
     * @param audioService Service interface for triggering platform-specific sound effects.
     * @param hapticsService Service interface for triggering platform-specific vibrations.
     * @param dealRegistry Registry for mapping between domain hand indices and layout-specific table coordinates.
     */
    suspend fun orchestrate(
        effects: Flow<GameEffect>,
        stateFlow: StateFlow<GameState>,
        animState: BlackjackAnimationState,
        audioService: AudioService,
        hapticsService: HapticsService,
        dealRegistry: DealAnimationRegistry,
    ) = coroutineScope {
        // flashJob is shared between the two pipelines so BigWin can cancel the state-driven flash.
        var flashJob: Job? = null
        launchEffectsPipeline(
            effects,
            animState,
            audioService,
            hapticsService,
            getFlashJob = { flashJob },
            setFlashJob = { flashJob = it }
        )
        launchStateDrivenAnimations(
            stateFlow,
            animState,
            dealRegistry,
            getFlashJob = { flashJob },
            setFlashJob = { flashJob = it }
        )
    }

    private fun CoroutineScope.launchEffectsPipeline(
        effects: Flow<GameEffect>,
        animState: BlackjackAnimationState,
        audioService: AudioService,
        hapticsService: HapticsService,
        getFlashJob: () -> Job?,
        setFlashJob: (Job?) -> Unit,
    ) {
        // 1. Effects pipeline — audio/haptics dispatching + animation state mutations
        launch {
            effects.collect { effect ->
                handleGameEffect(effect, hapticsService, audioService)
                when (effect) {
                    is GameEffect.NearMissHighlight ->
                        launch {
                            animState.nearMissHandIndex = effect.handIndex
                            delay(AnimationConstants.NearMissLifetimeMs)
                            animState.nearMissHandIndex = null
                        }
                    is GameEffect.ChipEruption ->
                        launch {
                            val instance = ChipEruptionInstance(Random.nextLong(), effect.amount, null)
                            animState.chipEruptions.add(instance)
                            delay(AnimationConstants.ChipEruptionLifetimeMs)
                            animState.chipEruptions.remove(instance)
                        }
                    is GameEffect.ChipLoss ->
                        launch {
                            val instance = ChipLossInstance(Random.nextLong(), effect.amount)
                            animState.chipLosses.add(instance)
                            delay(AnimationConstants.ChipLossLifetimeMs)
                            animState.chipLosses.remove(instance)
                        }
                    is GameEffect.BigWin -> {
                        getFlashJob()?.cancel()
                        animState.bigWinAmount = effect.totalPayout
                        animState.showBigWinBanner = true
                        // Banner lifetime is independent of the flash job — survives PLAYER_WON cancellation
                        launch {
                            delay(AnimationConstants.BigWinBannerLifetimeMs)
                            animState.showBigWinBanner = false
                        }
                        // Flash animation is stored as flashJob; may be cancelled by PLAYER_WON state handler
                        launch {
                            animState.flashColor = PrimaryGold
                            animState.flashAlpha.snapTo(0f)
                            animState.flashAlpha.animateTo(0.40f, tween(AnimationConstants.FlashInDuration))
                            animState.flashAlpha.animateTo(0f, tween(AnimationConstants.BigWinFlashOutDuration))
                        }.also { setFlashJob(it) }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun CoroutineScope.launchStateDrivenAnimations(
        stateFlow: StateFlow<GameState>,
        animState: BlackjackAnimationState,
        dealRegistry: DealAnimationRegistry,
        getFlashJob: () -> Job?,
        setFlashJob: (Job?) -> Unit,
    ) {
        // 2. State-driven flash and shake — cancels the previous animation job on each status change
        launch {
            var shakeJob: Job? = null
            stateFlow.distinctUntilChangedBy { it.status }.collect { state ->
                when (state.status) {
                    GameStatus.PLAYER_WON -> {
                        getFlashJob()?.cancel()
                        launch {
                            val isBlackjack = state.playerHands.any { it.isBlackjack }
                            animState.flashColor = if (isBlackjack) PrimaryGold else Color.White
                            val targetAlpha = if (isBlackjack) 0.25f else 0.15f
                            val outDuration =
                                if (isBlackjack) {
                                    AnimationConstants.FlashOutDurationBlackjack
                                } else {
                                    AnimationConstants.FlashOutDurationWin
                                }
                            animState.flashAlpha.animateTo(targetAlpha, tween(AnimationConstants.FlashInDuration))
                            animState.flashAlpha.animateTo(0f, tween(outDuration))
                        }.also { setFlashJob(it) }
                        // Animation orchestrator owns all payout mutations — keeps the screen
                        // composable free of win-detection and animState.activePayouts writes.
                        launch { triggerPayoutAnimations(state, animState, dealRegistry) }
                    }
                    GameStatus.DEALER_WON -> {
                        shakeJob?.cancel()
                        shakeJob =
                            launch {
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

    private suspend fun triggerPayoutAnimations(
        state: GameState,
        animState: BlackjackAnimationState,
        dealRegistry: DealAnimationRegistry,
    ) {
        delay(AnimationConstants.PayoutTriggerDelayMs)
        for (index in state.playerHands.indices) {
            val result = state.handResult(index)
            if (result == HandResult.WIN) {
                val bet = state.playerHands.getOrNull(index)?.bet ?: 0
                if (bet > 0) {
                    val zone = dealRegistry.tableLayout?.handZones?.getOrNull(index + 1)
                    val target =
                        if (zone != null) {
                            zone.clusterCenter + dealRegistry.gameplayAreaOffset
                        } else {
                            Offset.Zero
                        }
                    animState.activePayouts.add(PayoutInstance(Random.nextLong(), bet, target))
                }
            }
        }
    }
}
