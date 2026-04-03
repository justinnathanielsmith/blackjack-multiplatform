@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BlackjackStateMachineShutdownTest {
    @Test
    fun shutdownCausesEffectsFlowToComplete() =
        runTest {
            val sm = testMachine()

            sm.shutdown()
            sm.effects.test {
                awaitComplete()
            }
        }

    @Test
    fun dispatchAfterShutdown_isSilentlyIgnored() =
        runTest {
            val sm = testMachine()
            val stateBefore = sm.state.value

            sm.shutdown()
            advanceUntilIdle()

            // Should not throw; state must remain unchanged.
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            assertEquals(stateBefore, sm.state.value)
        }
}
