@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.middleware

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.bettingReadyToDeal
import io.github.smithjustinn.blackjack.util.bettingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingEffectsTest {
    @Test
    fun placeBetEmitsPlayPlinkSound() =
        runTest {
            val sm = testMachine(bettingState())

            sm.effects.test {
                sm.dispatch(GameAction.PlaceBet(100))
                advanceUntilIdle()
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetBetEmitsPlayPlinkSound() =
        runTest {
            val sm = testMachine(bettingReadyToDeal())

            sm.effects.test {
                sm.dispatch(GameAction.ResetBet)
                advanceUntilIdle()
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeSideBetEmitsPlayPlinkSound() =
        runTest {
            val sm = testMachine(bettingReadyToDeal(bet = 100, balance = 1000))

            sm.effects.test {
                sm.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 50))
                advanceUntilIdle()
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetSideBetEmitsPlayPlinkSound() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 950).copy(
                        sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 50)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.ResetSideBet(SideBetType.PERFECT_PAIRS))
                advanceUntilIdle()
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun invalidBetEmitsVibrate() =
        runTest {
            val sm = testMachine(bettingState(balance = 100))

            sm.effects.test {
                // Bet more than balance
                sm.dispatch(GameAction.PlaceBet(200))
                advanceUntilIdle()
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
