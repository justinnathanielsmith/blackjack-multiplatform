@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

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
    fun dispatchAfterShutdownThrowsException() =
        runTest {
            val sm = testMachine()

            sm.shutdown()

            // Wait a bit for the shutdown to propagate to the channel
            // Though shutdown() calls actionChannel.close() immediately.

            assertFailsWith<ClosedSendChannelException> {
                sm.dispatch(GameAction.Deal)
            }
        }
}
