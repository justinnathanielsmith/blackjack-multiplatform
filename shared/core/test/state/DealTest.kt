@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.util.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

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
    fun debug_channel_reading() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100))
                    )
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()
            assertTrue(true)
        }

    @Test
    fun deal_ignoredWhenNoBet() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0))
                    )
                )
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
            // New round-robin order: P1, D1, P2, D2
            // player NINE+TWO=11 (no BJ), dealer TEN+SEVEN=17 (no BJ)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = deckOf(Rank.NINE, Rank.TEN, Rank.TWO, Rank.SEVEN),
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
            // Interleaved: P1(NINE), D1(TEN), P2(TWO), D2(SEVEN): hole card hidden
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = deckOf(Rank.NINE, Rank.TEN, Rank.TWO, Rank.SEVEN),
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
            // Interleaved: P1(NINE), D1(KING), P2(TWO), D2(ACE): dealer wins immediately, hole card revealed
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = deckOf(Rank.NINE, Rank.KING, Rank.TWO, Rank.ACE),
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            // Dealer BJ → DEALER_WON
            assertFalse(state.dealerHand.cards[1].isFaceDown)
        }

    @Test
    fun sideBetPayout_perfectPairs_integration() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 50),
                    deck =
                        persistentListOf(
                            Card(Rank.TEN, Suit.SPADES), // P1
                            Card(Rank.SEVEN, Suit.HEARTS), // D1
                            Card(Rank.TEN, Suit.SPADES), // P2
                            Card(Rank.EIGHT, Suit.DIAMONDS), // D2
                        ),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Perfect Pair payout: 50 * 25 + 50 (original bet returned) = 1300 added to 1000
            assertEquals(2300, sm.state.value.balance)
            assertNotNull(sm.state.value.sideBetResults[SideBetType.PERFECT_PAIRS])
        }
}
