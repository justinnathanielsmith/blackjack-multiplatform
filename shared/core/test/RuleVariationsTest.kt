@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RuleVariationsTest {
    @Test
    fun testDealerStandsOnSoft17() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX), // soft 17
                        deck = deckOf(Rank.TWO),
                        rules = GameRules(dealerHitsSoft17 = false),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertEquals(2, state.dealerHand.cards.size)
            assertTrue(state.status != GameStatus.PLAYING && state.status != GameStatus.DEALER_TURN)
        }

    @Test
    fun testDealerHitsSoft17() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX), // soft 17
                        deck = deckOf(Rank.TWO),
                        rules = GameRules(dealerHitsSoft17 = true),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(19, state.dealerHand.score)
            assertEquals(3, state.dealerHand.cards.size)
        }

    @Test
    fun testBlackjackPayout_threeToTwo() =
        runTest {
            // balance=900 (bet already deducted), deal player BJ (ACE+TEN) vs dealer 17
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        rules = GameRules(blackjackPayout = BlackjackPayout.THREE_TO_TWO),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES),
                                Card(Rank.TEN, Suit.HEARTS), // Player BJ
                                Card(Rank.TEN, Suit.CLUBS),
                                Card(Rank.SEVEN, Suit.DIAMONDS), // Dealer 17
                            ),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // 900 + 100 (bet) + 150 (3:2 winnings) = 1150
            assertEquals(1150, sm.state.value.balance)
        }

    @Test
    fun testBlackjackPayout_sixToFive() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        rules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES),
                                Card(Rank.TEN, Suit.HEARTS), // Player BJ
                                Card(Rank.TEN, Suit.CLUBS),
                                Card(Rank.SEVEN, Suit.DIAMONDS), // Dealer 17
                            ),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // 900 + 100 (bet) + 120 (6:5 winnings) = 1120
            assertEquals(1120, sm.state.value.balance)
        }

    @Test
    fun testSurrender() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        balance = 900,
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Refund half (50). Balance: 900 + 50 = 950.
            assertEquals(950, sm.state.value.balance)
            assertEquals(GameStatus.DEALER_WON, sm.state.value.status)
        }

    @Test
    fun surrender_emitsChipLossForHalfBet() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        balance = 900,
                        bet = 100,
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            sm.effects.test {
                sm.dispatch(GameAction.Surrender)
                assertEquals(GameEffect.PlayLoseSound, awaitItem())
                assertEquals(GameEffect.ChipLoss(50), awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
