@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.middleware
import io.github.smithjustinn.blackjack.util.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import co.touchlab.kermit.Logger
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [GameFlowMiddleware], verifying the orchestration of asynchronous
 * game flows like the initial deal and dealer draw loop.
 */
class GameFlowMiddlewareTest {
    private val logger = Logger.withTag("GameFlowMiddlewareTest")

    @Test
    fun reshuffle_whenDeckIsEmpty() =
        runTest {
            val dispatchedActions = mutableListOf<GameAction>()
            val state =
                MutableStateFlow(
                    GameState(
                        status = GameStatus.BETTING,
                        deck = persistentListOf()
                    )
                )

            val middleware =
                GameFlowMiddleware(
                    state = state.asStateFlow(),
                    dispatch = { dispatchedActions.add(it) },
                    emitEffect = { },
                    isTest = true,
                    logger = logger
                )

            middleware.execute(ReducerCommand.RunDealSequence)

            // The first action should be SetDeck because it reshuffled an empty deck
            assertTrue(dispatchedActions.any { it is GameAction.SetDeck }, "Expected SetDeck action")
            val setDeckAction = dispatchedActions.first { it is GameAction.SetDeck } as GameAction.SetDeck
            assertEquals(BlackjackConfig.CARDS_PER_DECK * 6, setDeckAction.deck.size)
        }

    @Test
    fun dealSequence_interleavedCards() =
        runTest {
            val dispatchedActions = mutableListOf<GameAction>()
            val state =
                MutableStateFlow(
                    GameState(
                        status = GameStatus.BETTING,
                        handCount = 1,
                        deck = deckOf(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK)
                    )
                )

            val middleware =
                GameFlowMiddleware(
                    state = state.asStateFlow(),
                    dispatch = { dispatchedActions.add(it) },
                    emitEffect = { },
                    isTest = true,
                    logger = logger
                )

            middleware.execute(ReducerCommand.RunDealSequence)

            // Expected sequence: P0, D, P0, D, ApplyInitialOutcome
            assertEquals(5, dispatchedActions.size)
            assertTrue(dispatchedActions[0] is GameAction.DealCardToPlayer)
            assertEquals(0, (dispatchedActions[0] as GameAction.DealCardToPlayer).seatIndex)

            assertTrue(dispatchedActions[1] is GameAction.DealCardToDealer)
            assertEquals(false, (dispatchedActions[1] as GameAction.DealCardToDealer).faceDown)

            assertTrue(dispatchedActions[2] is GameAction.DealCardToPlayer)

            assertTrue(dispatchedActions[3] is GameAction.DealCardToDealer)
            assertEquals(true, (dispatchedActions[3] as GameAction.DealCardToDealer).faceDown)

            assertTrue(dispatchedActions[4] is GameAction.ApplyInitialOutcome)
        }

    @Test
    fun dealSequence_multiHand() =
        runTest {
            val dispatchedActions = mutableListOf<GameAction>()
            val state =
                MutableStateFlow(
                    GameState(
                        status = GameStatus.BETTING,
                        handCount = 2,
                        playerHands = persistentListOf(Hand(), Hand()),
                        deck = deckOf(Rank.ACE, Rank.KING, Rank.QUEEN, Rank.JACK, Rank.TEN, Rank.NINE)
                    )
                )

            val middleware =
                GameFlowMiddleware(
                    state = state.asStateFlow(),
                    dispatch = { dispatchedActions.add(it) },
                    emitEffect = { },
                    isTest = true,
                    logger = logger
                )

            middleware.execute(ReducerCommand.RunDealSequence)

            // Expected sequence: Round 1 (P0, P1, Dealer-up), Round 2 (P0, P1, Dealer-down), Terminal
            assertEquals(7, dispatchedActions.size)

            // Round 1
            assertEquals(GameAction.DealCardToPlayer(0), dispatchedActions[0])
            assertEquals(GameAction.DealCardToPlayer(1), dispatchedActions[1])
            assertEquals(GameAction.DealCardToDealer(faceDown = false), dispatchedActions[2])

            // Round 2
            assertEquals(GameAction.DealCardToPlayer(0), dispatchedActions[3])
            assertEquals(GameAction.DealCardToPlayer(1), dispatchedActions[4])
            assertEquals(GameAction.DealCardToDealer(faceDown = true), dispatchedActions[5])

            // Finalize initial deal
            assertEquals(GameAction.ApplyInitialOutcome, dispatchedActions[6])
        }

    @Test
    fun dealerTurn_loopUntilStanding() =
        runTest {
            val dispatchedActions = mutableListOf<GameAction>()
            // Dealer has hard 12 (10 + 2). Stand threshold is 17.
            // Deck provides: 3 (12+3=15), 5 (15+5=20 -> Stand)
            val state =
                MutableStateFlow(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = hand(Rank.TEN, Rank.TWO),
                        deck = deckOf(Rank.THREE, Rank.FIVE)
                    )
                )

            val middleware =
                GameFlowMiddleware(
                    state = state.asStateFlow(),
                    dispatch = { action ->
                        dispatchedActions.add(action)
                        // Simulate state update from action so the middleware see the new score
                        if (action is GameAction.DealerDraw) {
                            val currentHand = state.value.dealerHand
                            val nextCard = state.value.deck.first()
                            state.value =
                                state.value.copy(
                                    dealerHand = Hand(currentHand.cards.add(nextCard)),
                                    deck =
                                        state.value.deck
                                            .removeAt(0)
                                            .toPersistentList()
                                )
                        }
                    },
                    emitEffect = { },
                    isTest = true,
                    logger = logger
                )

            middleware.execute(ReducerCommand.RunDealerTurn)

            // Expected actions: RevealDealerHole, DealerDraw (3), DealerDraw (5), FinalizeGame
            assertEquals(4, dispatchedActions.size)
            assertEquals(GameAction.RevealDealerHole, dispatchedActions[0])
            assertEquals(GameAction.DealerDraw, dispatchedActions[1])
            assertEquals(GameAction.DealerDraw, dispatchedActions[2])
            assertEquals(GameAction.FinalizeGame, dispatchedActions[3])
        }

    @Test
    fun dealerTurn_emitsCriticalDrawEffect() =
        runTest {
            val emittedEffects = mutableListOf<GameEffect>()
            // Dealer has hard 16. Should emit DealerCriticalDraw before drawing.
            val state =
                MutableStateFlow(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = hand(Rank.TEN, Rank.SIX),
                        deck = deckOf(Rank.FOUR)
                    )
                )

            val middleware =
                GameFlowMiddleware(
                    state = state.asStateFlow(),
                    dispatch = { action ->
                        if (action is GameAction.DealerDraw) {
                            val currentHand = state.value.dealerHand
                            val nextCard = state.value.deck.first()
                            state.value =
                                state.value.copy(
                                    dealerHand = Hand(currentHand.cards.add(nextCard)),
                                    deck =
                                        state.value.deck
                                            .removeAt(0)
                                            .toPersistentList()
                                )
                        }
                    },
                    emitEffect = { emittedEffects.add(it) },
                    isTest = true,
                    logger = logger
                )

            middleware.execute(ReducerCommand.RunDealerTurn)

            assertTrue(GameEffect.DealerCriticalDraw in emittedEffects, "Expected DealerCriticalDraw effect")
        }
}
