package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MultiHandTest {

    @Test
    fun testInitialDeal_threeHands() = runTest {
        val stateMachine = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
        stateMachine.dispatch(GameAction.SelectHandCount(3))
        advanceUntilIdle()
        stateMachine.dispatch(GameAction.PlaceBet(100))
        advanceUntilIdle()
        
        // Balance: 1000 - (100 * 3) = 700
        assertEquals(700, stateMachine.state.value.balance)
        assertEquals(100, stateMachine.state.value.currentBet)

        stateMachine.dispatch(GameAction.Deal)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(3, state.playerHands.size)
        assertEquals(3, state.playerBets.size)
        state.playerHands.forEach { hand ->
            assertEquals(2, hand.cards.size)
        }
        assertEquals(0, state.activeHandIndex)
        assertEquals(GameStatus.PLAYING, state.status)
    }

    @Test
    fun testHandSwitching_onStand() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(
                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SEVEN, Suit.HEARTS))),
                Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS)))
            ),
            playerBets = persistentListOf(100, 100),
            activeHandIndex = 0,
            handCount = 2,
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.HEARTS), Card(Rank.SEVEN, Suit.SPADES, isFaceDown = true))),
            deck = persistentListOf(Card(Rank.TWO, Suit.CLUBS))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        assertEquals(1, stateMachine.state.value.activeHandIndex)
        assertEquals(GameStatus.PLAYING, stateMachine.state.value.status)
    }

    @Test
    fun testHandSwitching_onBust() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            playerHands = persistentListOf(
                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.FIVE, Suit.DIAMONDS)))
            ),
            playerBets = persistentListOf(100, 100),
            activeHandIndex = 0,
            handCount = 2,
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.HEARTS), Card(Rank.SEVEN, Suit.SPADES, isFaceDown = true))),
            deck = persistentListOf(Card(Rank.TEN, Suit.CLUBS)) // Bust card
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Hit)
        advanceUntilIdle()

        assertEquals(1, stateMachine.state.value.activeHandIndex)
        assertTrue(stateMachine.state.value.playerHands[0].isBust)
        assertEquals(GameStatus.PLAYING, stateMachine.state.value.status)
    }

    @Test
    fun testMultiHandPayout_mixedOutcomes() = runTest {
        // Hand 1 (Bust): Loss
        // Hand 2 (20 vs Dealer 17): Win
        // Hand 3 (17 vs Dealer 17): Push
        val initialState = GameState(
            status = GameStatus.PLAYING,
            balance = 700,
            playerHands = persistentListOf(
                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS), Card(Rank.TEN, Suit.CLUBS))), // 26 Bust
                Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS))), // 20 Win
                Hand(persistentListOf(Card(Rank.TEN, Suit.HEARTS), Card(Rank.SEVEN, Suit.SPADES))) // 17 Push
            ),
            playerBets = persistentListOf(100, 100, 100),
            activeHandIndex = 2, // Last hand
            handCount = 3,
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true))),
            deck = persistentListOf()
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Stand)
        advanceUntilIdle()

        // Payout:
        // Hand 1: 0
        // Hand 2: 200 (100 * 2)
        // Hand 3: 100 (Push)
        // Total Payout = 300
        // New Balance = 700 + 300 = 1000
        val state = stateMachine.state.value
        assertEquals(1000, state.balance)
        assertEquals(GameStatus.PLAYER_WON, state.status) // At least one win
    }

    @Test
    fun testSplitAces_oneCardOnly() = runTest {
        val initialState = GameState(
            status = GameStatus.PLAYING,
            balance = 900,
            playerHands = persistentListOf(
                Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.ACE, Suit.HEARTS)))
            ),
            playerBets = persistentListOf(100),
            activeHandIndex = 0,
            handCount = 1,
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true))),
            deck = persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS), Card(Rank.TWO, Suit.SPADES))
        )
        val stateMachine = BlackjackStateMachine(this, initialState)
        
        stateMachine.dispatch(GameAction.Split)
        advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals(2, state.playerHands.size)
        // Each ace hand got exactly one card and then turn advanced
        assertEquals(2, state.playerHands[0].cards.size)
        assertEquals(2, state.playerHands[1].cards.size)
        
        // Status should be DEALER_TURN or terminal (since both hands are finished)
        assertTrue(state.status != GameStatus.PLAYING)
    }

    @Test
    fun testSplitAces_noNaturalBlackjack() = runTest {
        // A + 10 after split is 21, but not a "Natural Blackjack" (pays 1:1 not 3:2)
        val initialState = GameState(
            status = GameStatus.PLAYING,
            balance = 900,
            playerHands = persistentListOf(
                Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.ACE, Suit.HEARTS)))
            ),
            playerBets = persistentListOf(100),
            activeHandIndex = 0,
            handCount = 1,
            dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS))), // Dealer stands on 16? No, dealer hits.
            // Let's force dealer to stand on 17.
            deck = persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS), Card(Rank.ACE, Suit.SPADES)) 
        )
        // Dealer cards: 10, 6, next is Ace -> 17.
        val stateMachine = BlackjackStateMachine(this, initialState.copy(dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS, isFaceDown = true)))))
        
        stateMachine.dispatch(GameAction.Split)
        advanceUntilIdle()

        // Hand 1: ACE + TEN = 21 (1:1 payout)
        // Hand 2: ACE + NINE = 20 (1:1 payout)
        // Total payout: (100 * 2) + (100 * 2) = 400
        // Balance: 800 (after split bet) + 400 = 1200
        assertEquals(1200, stateMachine.state.value.balance)
    }
}
