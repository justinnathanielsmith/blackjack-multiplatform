@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.middleware

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingEffectsTest {
    @Test
    fun placeBetEmitsPlayPlinkSound() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0))
                    )
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.PlaceBet(100))
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetBetEmitsPlayPlinkSound() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100))
                    )
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.ResetBet)
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeSideBetEmitsPlayPlinkSound() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100))
                    )
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 50))
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetSideBetEmitsPlayPlinkSound() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 950,
                        sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 50)
                    )
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.ResetSideBet(SideBetType.PERFECT_PAIRS))
                assertEquals(GameEffect.PlayPlinkSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun invalidBetEmitsVibrate() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 100,
                        playerHands = persistentListOf(Hand(bet = 0))
                    )
                )

            sm.effects.test {
                runCurrent()
                // Bet more than balance
                sm.dispatch(GameAction.PlaceBet(200))
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
