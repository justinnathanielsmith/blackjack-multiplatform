package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RuleVariationsTest {

    @Test
    fun testDealerStandsOnSoft17() = runTest {
        val rules = GameRules(dealerHitsSoft17 = false)
        val initialState = GameState(
            status = GameStatus.PLAYING,
            rules = rules,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS, isFaceDown = true))), // Soft 17
            deck = persistentListOf(Card(Rank.TWO, Suit.CLUBS))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(17, state.dealerHand.score)
        assertEquals(2, state.dealerHand.cards.size)
        assertTrue(state.status != GameStatus.PLAYING && state.status != GameStatus.DEALER_TURN)
    }

    @Test
    fun testDealerHitsSoft17() = runTest {
        val rules = GameRules(dealerHitsSoft17 = true)
        val initialState = GameState(
            status = GameStatus.PLAYING,
            rules = rules,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS, isFaceDown = true))), // Soft 17
            deck = persistentListOf(Card(Rank.TWO, Suit.CLUBS))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(19, state.dealerHand.score)
        assertEquals(3, state.dealerHand.cards.size)
    }

    @Test
    fun testBlackjackPayout_ThreeToTwo() = runTest {
        val rules = GameRules(blackjackPayout = BlackjackPayout.THREE_TO_TWO)
        val initialState = GameState(
            status = GameStatus.BETTING,
            balance = 900, // 1000 - 100 bet
            currentBet = 100,
            rules = rules,
            deck = persistentListOf(
                Card(Rank.ACE, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS), // Player BJ
                Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS) // Dealer 17
            )
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Deal)
        advanceUntilIdle()

        // Payout should be 3:2. 100 * 1.5 = 150 winnings + 100 bet = 250 back.
        // Balance: 900 + 250 = 1150.
        assertEquals(1150, stateMachine.state.value.balance)
    }

    @Test
    fun testBlackjackPayout_SixToFive() = runTest {
        val rules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
        val initialState = GameState(
            status = GameStatus.BETTING,
            balance = 900, // 1000 - 100 bet
            currentBet = 100,
            rules = rules,
            deck = persistentListOf(
                Card(Rank.ACE, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS), // Player BJ
                Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS) // Dealer 17
            )
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Deal)
        advanceUntilIdle()

        // Payout should be 6:5. 100 * 1.2 = 120 winnings + 100 bet = 220 back.
        // Balance: 900 + 220 = 1120.
        assertEquals(1120, stateMachine.state.value.balance)
    }

    @Test
    fun testSurrender() = runTest {
        val rules = GameRules(allowSurrender = true)
        val initialState = GameState(
            status = GameStatus.PLAYING,
            balance = 900,
            currentBet = 100,
            rules = rules,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true)))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Surrender)
        advanceUntilIdle()

        // Refund half (50). Balance: 900 + 50 = 950.
        assertEquals(950, stateMachine.state.value.balance)
        assertEquals(GameStatus.DEALER_WON, stateMachine.state.value.status)
    }
}
