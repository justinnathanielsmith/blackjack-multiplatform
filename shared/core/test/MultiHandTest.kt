@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MultiHandTest {
    @Test
    fun testInitialDeal_threeHands() =
        runTest {
            val deck =
                deckOf(
                    Rank.TEN,
                    Rank.NINE,
                    Rank.EIGHT, // P1,P2,P3 card1
                    Rank.TEN,
                    Rank.NINE,
                    Rank.EIGHT, // P1,P2,P3 card2
                    Rank.TEN,
                    Rank.SEVEN, // Dealer
                )
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0)),
                        deck = deck
                    ),
                )
            sm.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 0))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 1))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 2))
            advanceUntilIdle()

            // Balance: 1000 - 100 - 100 - 100 = 700
            assertEquals(700, sm.state.value.balance)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(3, state.playerHands.size)
            assertEquals(3, state.playerHands.size)
            state.playerHands.forEach { hand -> assertEquals(2, hand.cards.size) }
            assertEquals(0, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun testHandSwitching_onStand() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 900,
                    hands = listOf(hand(Rank.TEN, Rank.SEVEN), hand(Rank.TEN, Rank.SIX)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(1, sm.state.value.activeHandIndex)
            assertEquals(GameStatus.PLAYING, sm.state.value.status)
        }

    @Test
    fun testHandSwitching_onBust() =
        runTest {
            val initialState =
                multiHandPlayingState(
                    balance = 900,
                    hands = listOf(hand(Rank.TEN, Rank.SIX), hand(Rank.TEN, Rank.FIVE)),
                    bets = listOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TEN), // bust card
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(1, sm.state.value.activeHandIndex)
            assertTrue(
                sm.state.value.playerHands[0]
                    .isBust
            )
            assertEquals(GameStatus.PLAYING, sm.state.value.status)
        }

    @Test
    fun testMultiHandPayout_mixedOutcomes() =
        runTest {
            // Hand 1 (bust): 0; Hand 2 (20 vs dealer 17): 200; Hand 3 (17 vs dealer 17): 100 push
            // balance=700, total payout=300 → balance=1000
            val initialState =
                multiHandPlayingState(
                    balance = 700,
                    hands =
                        listOf(
                            Hand(persistentListOf(card(Rank.TEN), card(Rank.SIX), card(Rank.TEN))), // 26 bust
                            hand(Rank.TEN, Rank.TEN), // 20 win
                            hand(Rank.TEN, Rank.SEVEN), // 17 push
                        ),
                    bets = listOf(100, 100, 100),
                    activeHandIndex = 2,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN), // dealer 17
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertEquals(GameStatus.PLAYER_WON, state.status)
        }

    @Test
    fun testSplitAces_oneCardOnly() =
        runTest {
            val initialState =
                playingState(
                    balance = 900,
                    playerHand = hand(Rank.ACE, Rank.ACE),
                    dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TEN, Rank.NINE, Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands.size)
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(2, state.playerHands[1].cards.size)
            assertTrue(state.status != GameStatus.PLAYING)
        }

    @Test
    fun testSplitAces_noNaturalBlackjack() =
        runTest {
            // A+10 after split scores 21 but pays 1:1, not 3:2
            // Player: ACE+ACE (balance=900, bet=100)
            // Dealer: TEN+SIX face-down (will draw ACE → 17, stand)
            // Deck: TEN (hand1 → A+10=21), NINE (hand2 → A+9=20), ACE (dealer → 10+6+A=17)
            // After split: balance=800. Payouts: 200 + 200 = 400. Final: 1200
            val initialState =
                playingState(
                    balance = 900,
                    playerHand = hand(Rank.ACE, Rank.ACE),
                    dealerHand = dealerHand(Rank.TEN, Rank.SIX),
                    deck = deckOf(Rank.TEN, Rank.NINE, Rank.ACE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(1200, sm.state.value.balance)
        }
}
