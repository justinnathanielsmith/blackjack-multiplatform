@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.logic
import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.isTerminal
import io.github.smithjustinn.blackjack.util.card
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.multiHandPlayingState
import io.github.smithjustinn.blackjack.util.playingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SurrenderTest {
    @Test
    fun surrender_refundsHalfBet_and_endsTurn() =
        runTest {
            // Arrange: balance=900, bet=100, rules allow surrender
            val sm =
                testMachine(
                    playingState(
                        balance = 900,
                        bet = 100,
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true)
                    )
                )

            // Act
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(950, state.balance) // 900 + 100/2 = 950
            assertTrue(state.playerHands[0].isSurrendered)
            // Since it was the only hand, it should transition to terminal state or dealer turn then terminal
            // In the state machine, it calls advanceOrEndTurn which triggers dealer turn
            assertTrue(state.status.isTerminal())
        }

    @Test
    fun surrender_advancesToNextHand_inMultiHand() =
        runTest {
            // Arrange: 2 hands, active index 0
            val initialState =
                multiHandPlayingState(
                    balance = 800,
                    hands = listOf(hand(Rank.TEN, Rank.SIX), hand(Rank.TEN, Rank.SEVEN)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = dealerHand(Rank.TEN, Rank.SEVEN)
                ).copy(rules = GameRules(allowSurrender = true))

            val sm = testMachine(initialState)

            // Act
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(850, state.balance) // 800 + 100/2 = 850
            assertTrue(state.playerHands[0].isSurrendered)
            assertFalse(state.playerHands[1].isSurrendered)
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun surrender_rejected_ifRuleDisabled() =
        runTest {
            // Arrange: allowSurrender = false (default)
            val initialState =
                playingState(
                    balance = 900,
                    bet = 100,
                    playerHand = hand(Rank.TEN, Rank.SIX),
                    dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                    rules = GameRules(allowSurrender = false)
                )
            val sm = testMachine(initialState)

            // Act
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Assert
            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun surrender_rejected_ifMoreThanTwoCards() =
        runTest {
            // Arrange: 3 cards in hand
            val initialState =
                playingState(
                    balance = 900,
                    bet = 100,
                    playerHand = Hand(persistentListOf(card(Rank.FIVE), card(Rank.SIX), card(Rank.TWO))),
                    dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                    rules = GameRules(allowSurrender = true)
                )
            val sm = testMachine(initialState)

            // Act
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Assert
            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun surrender_rejected_inWrongStatus() =
        runTest {
            // Arrange: Status is BETTING
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 900,
                    playerHands = persistentListOf(Hand().copy(bet = 100)),
                    rules = GameRules(allowSurrender = true)
                )
            val sm = testMachine(initialState)

            // Act
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Assert
            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun surrender_emitsExpectedEffects() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        balance = 900,
                        bet = 100,
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Surrender)

                // Expected effects: PlayLoseSound and ChipLoss(50)
                val emitted =
                    buildList {
                        // We expect at least 2 items, but maybe more depending on state transitions (e.g. WinPulse if dealer busts, but here it ends)
                        // Actually handleSurrender specifically emits PlayLoseSound and ChipLoss
                        repeat(2) { add(awaitItem()) }
                    }

                assertTrue(GameEffect.PlayLoseSound in emitted)
                assertTrue(emitted.any { it is GameEffect.ChipLoss && it.amount == 50 })

                cancelAndIgnoreRemainingEvents()
            }
        }
}
