@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.SideBetLogic
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetOutcome
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SideBetEdgeTest {
    @Test
    fun placeSideBet_fails_with_insufficient_balance() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 100,
                    playerHands = persistentListOf(Hand(bet = 100)), // Main bet already placed
                    handCount = 1,
                    rules =
                        io.github.smithjustinn.blackjack.logic
                            .GameRules(deterministicReshuffle = true)
                )
            val sm = testMachine(initialState)

            // Try to place side bet of 25 (balance is 0 after main bet correctly accounted for)
            // Note: In GameState constructor, balance isn't auto-subtracted, I set it to 100 which is exactly the main bet.
            // So balance is already 100. Let's make it 0 to be sure.
            val stateAtZero = initialState.copy(balance = 0)
            val sm2 = testMachine(stateAtZero)

            sm2.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 25))
            advanceUntilIdle()

            // Balance should still be 0, sideBets empty
            assertEquals(0, sm2.state.value.balance)
            assertTrue(
                sm2.state.value.sideBets
                    .isEmpty()
            )
        }

    @Test
    fun sideBet_only_applies_to_hand_zero() =
        runTest {
            // Deck sets Hand 0 to non-pair, Hand 1 to Perfect Pair (if it was checked)
            // Deal sequence: P0, P1, D, P0, P1, D-hole
            // Correct deck order: 0:P0, 1:P1, 2:D, 3:P0, 4:P1, 5:D-hole
            val deck =
                deckOf(
                    Rank.TWO, // P0 card 1
                    Rank.ACE, // P1 card 1
                    Rank.TEN, // D upcard
                    Rank.THREE, // P0 card 2 (No pair)
                    Rank.ACE, // P1 card 2 (Perfect Pair - should be ignored)
                    Rank.TEN // D hole
                )

            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    handCount = 2,
                    playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                    deck = deck,
                    rules =
                        io.github.smithjustinn.blackjack.logic
                            .GameRules(deterministicReshuffle = true)
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Hand 0 is {TWO, THREE} -> No Pair. Hand 1 is {ACE, ACE} -> Pair but ignored.
            // Initial balance 1000. No subtraction occurred in setup.
            // sideBetUpdate.payoutTotal will be 0.
            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertTrue(state.sideBetResults.isEmpty())
        }

    @Test
    fun dealer_natural_bj_still_pays_side_bets() =
        runTest {
            // Player has Perfect Pair, Dealer has Natural BJ (TEN upcard to avoid Insurance phase)
            // Deck: 0:P, 1:D(TEN), 2:P(SAME), 3:D(ACE)
            val deck =
                deckOf(
                    Rank.KING, // P
                    Rank.TEN, // D (Upcard)
                    Rank.KING, // P (Perfect Pair)
                    Rank.ACE // D (Hole -> Natural BJ)
                )

            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                    deck = deck,
                    rules =
                        io.github.smithjustinn.blackjack.logic
                            .GameRules(deterministicReshuffle = true)
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // applyInitialOutcome resolves side bets before the dealer BJ is even revealed to the player
            // (though resolveInitialOutcomeValues sees it and sets status to DEALER_WON).
            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            // Side bet payout: 100 * 25 + 100 = 2600.
            // Initial 1000 + 2600 = 3600.
            assertEquals(3600, state.balance)
            assertEquals(SideBetOutcome.PERFECT_PAIR, state.sideBetResults[SideBetType.PERFECT_PAIRS]?.outcome)
        }

    @Test
    fun perfectPairs_coloredPair_boundaries() =
        runTest {
            // Test Red-Red (HEARTS-DIAMONDS)
            val card1 = Card(Rank.TEN, Suit.HEARTS)
            val card2 = Card(Rank.TEN, Suit.DIAMONDS)
            val handRed = Hand(persistentListOf(card1, card2))

            val resultRed = SideBetLogic.evaluatePerfectPairs(handRed)
            assertEquals(SideBetOutcome.COLORED_PAIR, resultRed?.outcome)
            assertEquals(12, resultRed?.payoutMultiplier)

            // Test Black-Black (SPADES-CLUBS)
            val card3 = Card(Rank.TEN, Suit.SPADES)
            val card4 = Card(Rank.TEN, Suit.CLUBS)
            val handBlack = Hand(persistentListOf(card3, card4))

            val resultBlack = SideBetLogic.evaluatePerfectPairs(handBlack)
            assertEquals(SideBetOutcome.COLORED_PAIR, resultBlack?.outcome)
            assertEquals(12, resultBlack?.payoutMultiplier)
        }

    @Test
    fun twentyOnePlusThree_suitedTriple_ace() =
        runTest {
            val playerHand =
                Hand(
                    persistentListOf(
                        Card(Rank.ACE, Suit.SPADES),
                        Card(Rank.ACE, Suit.SPADES)
                    )
                )
            val dealerUpcard = Card(Rank.ACE, Suit.SPADES)

            val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
            assertEquals(SideBetOutcome.SUITED_TRIPLE, result?.outcome)
            assertEquals(100, result?.payoutMultiplier)
        }

    @Test
    fun twentyOnePlusThree_straightFlush_aceLow() =
        runTest {
            // A, 2, 3 in Spades
            val playerHand =
                Hand(
                    persistentListOf(
                        Card(Rank.ACE, Suit.SPADES),
                        Card(Rank.TWO, Suit.SPADES)
                    )
                )
            val dealerUpcard = Card(Rank.THREE, Suit.SPADES)

            val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
            assertEquals(SideBetOutcome.STRAIGHT_FLUSH, result?.outcome)
            assertEquals(40, result?.payoutMultiplier)
        }

    @Test
    fun twentyOnePlusThree_straight_aceHigh() =
        runTest {
            // Q, K, A (Mixed Suits)
            val playerHand =
                Hand(
                    persistentListOf(
                        Card(Rank.QUEEN, Suit.SPADES),
                        Card(Rank.KING, Suit.HEARTS)
                    )
                )
            val dealerUpcard = Card(Rank.ACE, Suit.DIAMONDS)

            val result = SideBetLogic.evaluateTwentyOnePlusThree(playerHand, dealerUpcard)
            assertEquals(SideBetOutcome.STRAIGHT, result?.outcome)
            assertEquals(10, result?.payoutMultiplier)
        }
}
