package io.github.smithjustinn.blackjack.ui.animation

import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BlackjackAnimationOrchestratorTest {
    @Test
    fun chipEruption_addsToListThenRemovesAfterLifetime() =
        runTest {
            val animState = BlackjackAnimationState()
            val effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
            val stateFlow =
                MutableStateFlow(
                    GameState(status = GameStatus.PLAYING)
                )

            val job =
                launch {
                    BlackjackAnimationOrchestrator.orchestrate(
                        effects = effects,
                        stateFlow = stateFlow,
                        animState = animState,
                        onEffect = {},
                        dealRegistry = DealAnimationRegistry(),
                    )
                }

            // Let the orchestrate coroutines start collecting
            advanceUntilIdle()

            // Emit a ChipEruption effect
            effects.emit(GameEffect.ChipEruption(amount = 25))

            // Advance time to let the collect handler run and launch the inner coroutine
            advanceTimeBy(10)

            // List should have grown by one
            assertEquals(1, animState.chipEruptions.size, "chipEruptions should contain 1 item after emission")
            assertEquals(25, animState.chipEruptions.first().amount, "ChipEruption amount should match")

            // Advance time past the lifetime
            advanceTimeBy(AnimationConstants.ChipEruptionLifetimeMs + 100)

            // List should be empty after lifetime expires
            assertTrue(animState.chipEruptions.isEmpty(), "chipEruptions should be empty after lifetime expires")

            job.cancel()
        }

    @Test
    fun chipLoss_addsToListThenRemovesAfterLifetime() =
        runTest {
            val animState = BlackjackAnimationState()
            val effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
            val stateFlow =
                MutableStateFlow(
                    GameState(status = GameStatus.PLAYING)
                )

            val job =
                launch {
                    BlackjackAnimationOrchestrator.orchestrate(
                        effects = effects,
                        stateFlow = stateFlow,
                        animState = animState,
                        onEffect = {},
                        dealRegistry = DealAnimationRegistry(),
                    )
                }

            advanceUntilIdle()
            effects.emit(GameEffect.ChipLoss(amount = 50))
            advanceTimeBy(10)

            assertEquals(1, animState.chipLosses.size, "chipLosses should contain 1 item after emission")
            assertEquals(50, animState.chipLosses.first().amount, "ChipLoss amount should match")

            advanceTimeBy(AnimationConstants.ChipLossLifetimeMs + 100)

            assertTrue(animState.chipLosses.isEmpty(), "chipLosses should be empty after lifetime expires")

            job.cancel()
        }

    @Test
    fun nearMiss_setsHandIndexThenClearsAfterLifetime() =
        runTest {
            val animState = BlackjackAnimationState()
            val effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 1)
            val stateFlow =
                MutableStateFlow(
                    GameState(status = GameStatus.PLAYING)
                )

            val job =
                launch {
                    BlackjackAnimationOrchestrator.orchestrate(
                        effects = effects,
                        stateFlow = stateFlow,
                        animState = animState,
                        onEffect = {},
                        dealRegistry = DealAnimationRegistry(),
                    )
                }

            advanceUntilIdle()
            effects.emit(GameEffect.NearMissHighlight(handIndex = 1))
            advanceTimeBy(10)

            assertEquals(1, animState.nearMissHandIndex, "nearMissHandIndex should be set after emission")

            advanceTimeBy(AnimationConstants.NearMissLifetimeMs + 100)

            assertEquals(null, animState.nearMissHandIndex, "nearMissHandIndex should clear after lifetime expires")

            job.cancel()
        }
}
