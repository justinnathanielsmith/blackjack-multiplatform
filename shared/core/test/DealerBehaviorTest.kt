@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DealerBehaviorTest {
    @Test
    fun testDealerStandsOnHard17() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FIVE), // should not be drawn
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertEquals(2, state.dealerHand.cards.size)
            assertFalse(state.dealerHand.isSoft)
            assertEquals(GameStatus.PLAYER_WON, state.status)
        }

    @Test
    fun testDealerBusts() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.SIX, Rank.SIX), // 12
                        deck = deckOf(Rank.TEN), // 12 + 10 = 22 bust
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertTrue(state.dealerHand.isBust)
            assertEquals(22, state.dealerHand.score)
            assertEquals(GameStatus.PLAYER_WON, state.status)
        }

    @Test
    fun testDealerMultiHitTo17() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        balance = 900,
                        playerHand = hand(Rank.EIGHT, Rank.EIGHT), // 16
                        dealerHand = dealerHand(Rank.TWO, Rank.THREE), // 5
                        deck = deckOf(Rank.FIVE, Rank.SEVEN), // 5+5=10, 10+7=17
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertEquals(4, state.dealerHand.cards.size)
            assertEquals(GameStatus.DEALER_WON, state.status)
        }

    @Test
    fun testDealerHoleCardRevealNoHit() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.EIGHT), // 18
                        deck = deckOf(Rank.FIVE),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(18, state.dealerHand.score)
            assertEquals(2, state.dealerHand.cards.size)
            assertFalse(state.dealerHand.cards[1].isFaceDown)
        }

    @Test
    fun testDealerNaturalBlackjackResolution() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SEVEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.KING), // BJ
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(21, state.dealerHand.score)
        }

    @Test
    fun testDealerHitsSoft17EdgeCase() =
        runTest {
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.TWO), // A+2 = soft 13, after hit A+2+4 = soft 17
                        // A+2+4=soft17 → hits; +5=22→12(hard) → hits; +6=18 → stands
                        deck = deckOf(Rank.FOUR, Rank.FIVE, Rank.SIX),
                        rules = rules,
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(18, state.dealerHand.score)
            assertEquals(5, state.dealerHand.cards.size)
        }
}
