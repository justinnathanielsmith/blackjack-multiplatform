@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionEnforcementTest {
    @Test
    fun doubleDown_fails_withInsufficientBalance() =
        runTest {
            // Player has $50 balance, bet is $100 -> cannot double
            // Card is Rank.TEN + Rank.ACE = 11 (hard total 11) - valid for double
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.SIX, Rank.FIVE).copy(bet = 100),
                        dealerHand = dealerHand(Rank.TEN, Rank.TEN),
                        balance = 50, // Insufficient for second $100
                        deck = deckOf(Rank.TEN)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.DoubleDown)
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val state = sm.state.value
            assertEquals(50, state.balance)
            assertEquals(1, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun split_fails_withInsufficientBalance() =
        runTest {
            // Player has $50 balance, bet is $100 -> cannot split
            // Pair of Rank.EIGHTs
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.EIGHT).copy(bet = 100),
                        dealerHand = dealerHand(Rank.TEN, Rank.TEN),
                        balance = 50,
                        deck = deckOf(Rank.TEN, Rank.TEN)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Split)
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val state = sm.state.value
            assertEquals(50, state.balance)
            assertEquals(1, state.playerHands.size)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun takeInsurance_fails_withInsufficientBalance() =
        runTest {
            // Dealer shows ACE, player bet is $100, balance is $20 (need $50 for insurance)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 20,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.TEN).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.TakeInsurance)
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            val state = sm.state.value
            assertEquals(20, state.balance)
            assertEquals(0, state.insuranceBet)
            assertEquals(GameStatus.INSURANCE_OFFERED, state.status)
        }

    @Test
    fun split_deductsBalance_and_createsSecondHand() =
        runTest {
            // Player has $1000 balance, bet is $100 -> can split
            // Pair of Rank.TENs
            // Deck provides TWO cards (one for each new hand)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                        dealerHand = dealerHand(Rank.TEN, Rank.TEN),
                        balance = 1000,
                        deck = deckOf(Rank.TWO, Rank.THREE)
                    )
                )

            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(900, state.balance) // 1000 - 100 (for second hand)
            assertEquals(2, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(100, state.playerHands[1].bet)

            // Hand 0: TEN (from first hand) + TWO (from deck)
            assertEquals(12, state.playerHands[0].score)
            // Hand 1: TEN (from first hand) + THREE (from deck)
            assertEquals(13, state.playerHands[1].score)
        }

    @Test
    fun split_aces_allows_only_one_card_then_stands() =
        runTest {
            // Pair of Aces
            // Deck provides two cards (Seven and Eight)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.ACE, Rank.ACE).copy(bet = 100),
                        dealerHand = dealerHand(Rank.TEN, Rank.TEN),
                        balance = 1000,
                        deck = deckOf(Rank.SEVEN, Rank.EIGHT)
                    )
                )

            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands.size)
            // Hand 0: ACE + SEVEN = 18
            assertEquals(18, state.playerHands[0].score)
            // Hand 1: ACE + EIGHT = 19
            assertEquals(19, state.playerHands[1].score)

            // Turn should have advanced past player (DEALER_TURN or terminal)
            // In this case, dealer has TEN + TEN = 20, so dealer wins.
            assertEquals(GameStatus.DEALER_WON, state.status)

            // Try to hit - should be ignored (status is not PLAYING)
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()
            assertEquals(GameStatus.DEALER_WON, sm.state.value.status)
        }
}
