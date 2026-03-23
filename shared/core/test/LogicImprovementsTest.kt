@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class LogicImprovementsTest {
    @Test
    fun placeBet_emitsVibrate_whenInsufficientBalance() =
        runTest {
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 50, playerHands = persistentListOf(Hand(bet = 0)))
                )
            sm.effects.test {
                sm.dispatch(GameAction.PlaceBet(100))
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_emitsVibrate_whenSeatIndexOutOfBounds() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0)),
                        handCount = 1
                    )
                )
            sm.effects.test {
                sm.dispatch(GameAction.PlaceBet(100, seatIndex = 5))
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_emitsVibrate_whenInvalidCount() =
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
                sm.dispatch(GameAction.SelectHandCount(4))
                assertEquals(GameEffect.Vibrate, awaitItem())
                sm.dispatch(GameAction.SelectHandCount(0))
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun handleDeal_reshuffles_whenDeckIsEmpty() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = persistentListOf()
                    )
                )

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // After deal, if it reshuffled: 312 total - 4 dealt = 308
            assertEquals(308, sm.state.value.deck.size)
        }
}
