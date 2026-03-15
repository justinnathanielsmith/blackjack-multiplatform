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
class DealerBehaviorTest {

    @Test
    fun testDealerStandsOnHard17() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true))),
            deck = persistentListOf(Card(Rank.FIVE, Suit.SPADES)) // Should not be drawn
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(17, state.dealerHand.score)
        assertEquals(2, state.dealerHand.cards.size)
        assertFalse(state.dealerHand.isSoft)
        assertEquals(GameStatus.PLAYER_WON, state.status) // Player 20 vs Dealer 17
    }

    @Test
    fun testDealerBusts() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.SIX, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS, isFaceDown = true))), // 12
            deck = persistentListOf(Card(Rank.TEN, Suit.HEARTS)) // 12 + 10 = 22 (Bust)
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertTrue(state.dealerHand.isBust)
        assertEquals(22, state.dealerHand.score)
        assertEquals(GameStatus.PLAYER_WON, state.status)
    }

    @Test
    fun testDealerMultiHitTo17() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.EIGHT, Suit.HEARTS)))), // 16
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.TWO, Suit.CLUBS), Card(Rank.THREE, Suit.DIAMONDS, isFaceDown = true))), // 5
            deck = persistentListOf(
                Card(Rank.FIVE, Suit.HEARTS), // 5 + 5 = 10
                Card(Rank.SEVEN, Suit.SPADES) // 10 + 7 = 17
            )
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(17, state.dealerHand.score)
        assertEquals(4, state.dealerHand.cards.size)
        assertEquals(GameStatus.DEALER_WON, state.status) // Dealer 17 vs Player 16
    }

    @Test
    fun testDealerHoleCardRevealNoHit() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.EIGHT, Suit.DIAMONDS, isFaceDown = true))), // 18
            deck = persistentListOf(Card(Rank.FIVE, Suit.SPADES))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(18, state.dealerHand.score)
        assertEquals(2, state.dealerHand.cards.size)
        assertFalse(state.dealerHand.cards[1].isFaceDown)
    }

    @Test
    fun testDealerNaturalBlackjackResolution() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SEVEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.ACE, Suit.CLUBS), Card(Rank.KING, Suit.DIAMONDS, isFaceDown = true))), // BJ
            deck = persistentListOf()
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(GameStatus.DEALER_WON, state.status)
        assertEquals(21, state.dealerHand.score)
    }

    @Test
    fun testDealerHitsSoft17EdgeCase() = runTest {
        val rules = GameRules(dealerHitsSoft17 = true)
        val initialState = GameState(
            status = GameStatus.PLAYING,
            rules = rules,
            playerHands = persistentListOf(Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))),
            playerBets = persistentListOf(100),
            dealerHand = Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.TWO, Suit.HEARTS, isFaceDown = true))), 
            deck = persistentListOf(
                Card(Rank.FOUR, Suit.CLUBS), // A+2+4 = 17 (Soft) -> HITS
                Card(Rank.FIVE, Suit.DIAMONDS), // 17+5 = 22 -> 12 (Hard) -> HITS
                Card(Rank.SIX, Suit.SPADES) // 12+6 = 18 (Hard) -> STANDS
            )
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(18, state.dealerHand.score)
        assertEquals(5, state.dealerHand.cards.size) // A, 2, 4, 5, 6
    }
}
