@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DealTest {
    @Test
    fun deal_ignoredWhenNoBet() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.BETTING, state.status)
            assertEquals(0, state.playerHands[0].cards.size)
            assertEquals(0, state.dealerHand.cards.size)
        }

    @Test
    fun initialDeal_deals2CardsToPlayerAnd2ToDealer() =
        runTest {
            // Controlled deck: player NINE+TWO=11 (no BJ), dealer TEN+SEVEN=17 (no BJ)
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        currentBet = 100,
                        deck = deckOf(Rank.NINE, Rank.TWO, Rank.TEN, Rank.SEVEN),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(0, state.deck.size)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun deal_dealerHoleCardIsFaceDown_whenPlaying() =
        runTest {
            // player NINE+TWO=11, dealer TEN+SEVEN=17: game proceeds normally, hole card hidden
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        currentBet = 100,
                        deck = deckOf(Rank.NINE, Rank.TWO, Rank.TEN, Rank.SEVEN),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYING, state.status)
            assertTrue(state.dealerHand.cards[1].isFaceDown)
        }

    @Test
    fun deal_dealerHoleCardRevealed_onDealerBlackjack() =
        runTest {
            // player NINE+TWO=11, dealer ACE+KING=BJ: dealer wins immediately, hole card revealed
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        currentBet = 100,
                        deck = deckOf(Rank.NINE, Rank.TWO, Rank.ACE, Rank.KING),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            // Dealer BJ → DEALER_WON (or INSURANCE_OFFERED if ace is up, followed by resolution)
            assertFalse(state.dealerHand.cards[1].isFaceDown)
        }

    @Test
    fun sideBetPayout_perfectPairs_integration() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    currentBet = 100,
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 50),
                    deck =
                        persistentListOf(
                            Card(Rank.TEN, Suit.SPADES),
                            Card(Rank.TEN, Suit.SPADES),
                            Card(Rank.SEVEN, Suit.HEARTS),
                            Card(Rank.EIGHT, Suit.DIAMONDS),
                        ),
                )
            val sm = BlackjackStateMachine(this, initialState)
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Perfect Pair payout: 50 * 25 = 1250 added to 1000
            assertEquals(2250, sm.state.value.balance)
            assertNotNull(sm.state.value.sideBetResults[SideBetType.PERFECT_PAIRS])
        }
}
