@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
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
