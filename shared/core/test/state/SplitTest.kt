@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SplitTest {
    @Test
    fun split_createsTwoHands() =
        runTest {
            val initialState =
                playingState(
                    playerHand = hand(Rank.EIGHT, Rank.EIGHT),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO, Rank.THREE, Rank.FOUR),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(
                2,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.size
            )
            assertEquals(Rank.EIGHT, state.playerHands[0].cards[0].rank)
            assertEquals(
                Rank.EIGHT,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.get(0)
                    ?.rank
            )
            assertEquals(1, state.deck.size) // started with 3, used 2
        }

    @Test
    fun split_deductsBalance() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.EIGHT),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TWO, Rank.THREE),
                    ),
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(800, state.balance)
            assertEquals(100, state.playerHands.getOrNull(1)?.bet)
        }

    @Test
    fun split_invalid_nonPair() =
        runTest {
            val initialState =
                playingState(
                    playerHand = hand(Rank.EIGHT, Rank.NINE),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO, Rank.THREE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun split_invalid_insufficientBalance() =
        runTest {
            val initialState =
                playingState(
                    balance = 50,
                    playerHand = hand(Rank.EIGHT, Rank.EIGHT),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO, Rank.THREE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun split_invalid_afterHit() =
        runTest {
            val initialState =
                playingState(
                    playerHand = Hand(persistentListOf(card(Rank.EIGHT), card(Rank.EIGHT), card(Rank.TWO))),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO, Rank.THREE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun split_invalid_whenAlreadySplit() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.EIGHT, Rank.TWO).copy(bet = 100),
                            hand(Rank.EIGHT, Rank.THREE).copy(bet = 100),
                        ),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.FOUR, Rank.FIVE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun splitAce_noExtraHit() =
        runTest {
            // Split ace hands block further hits
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            Hand(
                                persistentListOf(card(Rank.ACE), card(Rank.FIVE)),
                                bet = 100,
                                wasSplit = true,
                                isFromSplitAce = true
                            ),
                            Hand(
                                persistentListOf(card(Rank.ACE), card(Rank.THREE)),
                                bet = 100,
                                wasSplit = true,
                                isFromSplitAce = true
                            ),
                        ),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun splitAce_autoStandsAfterDeal() =
        runTest {
            val initialState =
                playingState(
                    playerHand = hand(Rank.ACE, Rank.ACE),
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.FIVE, Rank.THREE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun split_independentPayouts_primaryWinsSplitLoses() =
        runTest {
            // Primary TEN+TEN=20 wins vs dealer 18; Split EIGHT+EIGHT+SIX=22 busts
            // balance=800, bets=[100,100] → balance += 200
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                            Hand(persistentListOf(card(Rank.EIGHT), card(Rank.EIGHT), card(Rank.SIX)), bet = 100),
                        ),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.EIGHT),
                    deck = persistentListOf(),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1000, state.balance)
        }

    @Test
    fun split_independentPayouts_bothWin() =
        runTest {
            // Primary TEN+TEN=20, Split TEN+NINE=19, dealer TEN+SIX draws KING → 26 bust
            // balance=800, bets=[100,100] → balance += 200 + 200
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                            hand(Rank.TEN, Rank.NINE).copy(bet = 100),
                        ),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.SIX),
                    deck = deckOf(Rank.KING),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1200, state.balance)
        }

    @Test
    fun split_insertsHandAtActiveIndexPlusOne() =
        runTest {
            // 2 initial hands; split hand0 (pair of 8s) → 3 hands, new split hand at index 1
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.EIGHT, Rank.EIGHT).copy(bet = 100),
                            hand(Rank.SEVEN, Rank.THREE).copy(bet = 100),
                        ),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.TWO, Rank.THREE, Rank.FOUR),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(3, state.playerHands.size)
            assertEquals(Rank.EIGHT, state.playerHands[1].cards[0].rank)
        }

    @Test
    fun splitAce_turnProgression() =
        runTest {
            // Hand 0 (pair of aces) splits → gets 1 card each → Hand 2 (original Hand 1) becomes active
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 700,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.ACE, Rank.ACE).copy(bet = 100),
                            hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                        ),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    deck = deckOf(Rank.FIVE, Rank.THREE),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(3, state.playerHands.size)
            assertEquals(Rank.FIVE, state.playerHands[0].cards[1].rank)
            assertEquals(Rank.THREE, state.playerHands[1].cards[1].rank)
            assertEquals(2, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun bust_onPrimaryHand_advancesToSplit() =
        runTest {
            // Primary TEN+FIVE=15, hit TEN → 25 bust → split hand becomes active
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.TEN, Rank.FIVE).copy(bet = 100),
                            hand(Rank.TEN, Rank.THREE).copy(bet = 100),
                        ),
                    activeHandIndex = 0,
                    dealerHand = hand(Rank.SEVEN, Rank.SEVEN),
                    deck = deckOf(Rank.TEN),
                )
            val sm =
                testMachine(
                    initialState
                )
            sm.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
            assertTrue(state.playerHands[0].isBust)
        }
}
