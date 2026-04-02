@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DoubleDownTest {
    @Test
    fun doubleDown_doublesbet_and_deals_one_card() =
        runTest {
            // Player FIVE+SIX=11, draws TWO → 13
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.FIVE, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                        deck = deckOf(Rank.TWO),
                    ),
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(200, state.playerHands[0].bet)
            assertEquals(800, state.balance)
            assertEquals(3, state.playerHands[0].cards.size)
            assertEquals(0, state.deck.size)
        }

    @Test
    fun doubleDown_transitions_to_DEALER_WON() =
        runTest {
            // Player FIVE+SIX=11, draws TWO → 13, dealer TEN+SEVEN=17 → DEALER_WON
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.FIVE, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TWO),
                    ),
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(GameStatus.DEALER_WON, sm.state.value.status)
        }

    @Test
    fun doubleDown_bust_is_DEALER_WON() =
        runTest {
            // Player NINE+SIX=15, draws TEN → 25 bust
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.NINE, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TEN),
                    ),
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertTrue(state.playerHands[0].isBust)
        }

    @Test
    fun doubleDown_invalid_after_hit() =
        runTest {
            // 3-card hand: cannot double down
            val initialState =
                playingState(
                    playerHand = Hand(persistentListOf(card(Rank.FIVE), card(Rank.SIX), card(Rank.TWO))),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun doubleDown_invalid_if_insufficient_balance() =
        runTest {
            val initialState =
                playingState(
                    balance = 99,
                    bet = 100,
                    playerHand = hand(Rank.FIVE, Rank.SIX),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun doubleDown_invalid_in_wrong_status() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 900,
// removed:                     currentBets = persistentListOf(100),
                    playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun doubleDown_updatesActiveBetOnly() =
        runTest {
            // 3-hand game, activeHandIndex=1: only playerBets[1] should be doubled
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    handCount = 3,
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.NINE).copy(bet = 100),
                            hand(Rank.FIVE, Rank.SIX).copy(bet = 100),
                            hand(Rank.EIGHT, Rank.SEVEN).copy(bet = 100),
                        ),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(200, state.playerHands[1].bet)
            assertEquals(100, state.playerHands[2].bet)
        }

    @Test
    fun doubleDown_allowedOnNonFirstHandInMultiHandGame() =
        runTest {
            // Multi-hand deal (handCount=3, no splits), allowDoubleAfterSplit=false — double still valid
            val state =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    handCount = 3,
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.NINE).copy(bet = 100),
                            hand(Rank.TEN, Rank.EIGHT).copy(bet = 100),
                            hand(Rank.FIVE, Rank.SIX).copy(bet = 100),
                        ),
                    activeHandIndex = 2,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                    rules = GameRules(allowDoubleAfterSplit = false),
                )
            assertTrue(state.canDoubleDown())
        }

    @Test
    fun doubleAfterSplit_allowed() =
        runTest {
            val state =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 1000,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                            hand(Rank.FIVE, Rank.SIX).copy(bet = 100),
                        ),
                    activeHandIndex = 1,
                    rules = GameRules(allowDoubleAfterSplit = true),
                )
            assertTrue(state.canDoubleDown())
        }

    @Test
    fun doubleAfterSplit_disallowed() =
        runTest {
            val state =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 1000,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(card(Rank.TEN), card(Rank.TEN)), bet = 100, wasSplit = true),
                            Hand(persistentListOf(card(Rank.FIVE), card(Rank.SIX)), bet = 100, wasSplit = true),
                        ),
                    activeHandIndex = 1,
                    rules = GameRules(allowDoubleAfterSplit = false),
                )
            assertFalse(state.canDoubleDown())
        }

    @Test
    fun doubleAfterSplit_respectsRules() =
        runTest {
            val state1 =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 1000,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(card(Rank.TEN), card(Rank.FIVE)), bet = 100, wasSplit = true),
                        ),
                    rules = GameRules(allowDoubleAfterSplit = true),
                )
            assertTrue(state1.canDoubleDown(), "Should allow double after split when rules permit")

            val state2 = state1.copy(rules = GameRules(allowDoubleAfterSplit = false))
            assertFalse(state2.canDoubleDown(), "Should NOT allow double after split when rules forbid")
        }

    @Test
    fun canDoubleDown_true_when_two_cards_and_sufficient_balance_and_not_split() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                balance = 100
            )
        assertTrue(state.canDoubleDown())
    }

    @Test
    fun canDoubleDown_false_when_more_than_two_cards() {
        val state =
            GameState(
                playerHands =
                    persistentListOf(
                        hand(Rank.FIVE, Rank.SIX, Rank.TWO).copy(bet = 100)
                    ),
                balance = 100
            )
        assertFalse(state.canDoubleDown())
    }

    @Test
    fun canDoubleDown_false_when_less_than_two_cards() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.FIVE).copy(bet = 100)),
                balance = 100
            )
        assertFalse(state.canDoubleDown())
    }

    @Test
    fun canDoubleDown_false_when_insufficient_balance() {
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                balance = 99
            )
        assertFalse(state.canDoubleDown())
    }

    @Test
    fun canDoubleDown_uses_active_hand_and_bet() {
        val validHand = hand(Rank.FIVE, Rank.SIX)
        val invalidHand = hand(Rank.FIVE, Rank.SIX, Rank.TWO)

        val state =
            GameState(
                playerHands =
                    persistentListOf(
                        invalidHand.copy(bet = 100),
                        validHand.copy(bet = 200)
                    ),
                balance = 200,
                activeHandIndex = 1
            )
        assertTrue(state.canDoubleDown(), "Should use active hand (index 1) which has 2 cards and sufficient balance")

        val stateInsufficientBalanceForActive =
            GameState(
                playerHands =
                    persistentListOf(
                        invalidHand.copy(bet = 100),
                        validHand.copy(bet = 200)
                    ),
                balance = 150,
                activeHandIndex = 1
            )
        assertFalse(stateInsufficientBalanceForActive.canDoubleDown(), "Balance (150) is less than active bet (200)")
    }
}
