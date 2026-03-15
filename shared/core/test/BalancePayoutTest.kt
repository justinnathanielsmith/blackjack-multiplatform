@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BalancePayoutTest {
    // ── Blackjack payouts ─────────────────────────────────────────────────────

    @Test
    fun blackjackPayout_3to2() =
        runTest {
            // Player ACE+TEN=BJ, dealer TEN+EIGHT=18: 3:2 payout
            // balance=1000, bet=100 → payout = 100 + 150 = 250 → balance=1250
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        currentBet = 100,
                        rules = GameRules(blackjackPayout = BlackjackPayout.THREE_TO_TWO),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES),
                                Card(Rank.TEN, Suit.HEARTS),
                                Card(Rank.TEN, Suit.CLUBS),
                                Card(Rank.EIGHT, Suit.DIAMONDS),
                            ),
                        handCount = 1,
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1250, state.balance)
        }

    @Test
    fun blackjackPayout_6to5() =
        runTest {
            // Player ACE+TEN=BJ, dealer TEN+EIGHT=18: 6:5 payout
            // balance=1000, bet=100 → payout = 100 + 120 = 220 → balance=1220
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        currentBet = 100,
                        rules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES),
                                Card(Rank.TEN, Suit.HEARTS),
                                Card(Rank.TEN, Suit.CLUBS),
                                Card(Rank.EIGHT, Suit.DIAMONDS),
                            ),
                        handCount = 1,
                    ),
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1220, state.balance)
        }

    @Test
    fun naturalBlackjack_3to2_multiHand() =
        runTest {
            // Hand0: BJ (ACE+TEN) → 3:2 payout = 250; Hand1: 20 (TEN+TEN) vs dealer 18 → 200
            // balance=800, bets=[100,100] → balance = 800 + 250 + 200 = 1250
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            hand(Rank.ACE, Rank.TEN),
                            hand(Rank.TEN, Rank.TEN),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = hand(Rank.TEN, Rank.EIGHT),
                    deck = persistentListOf(),
                )
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState)
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(1250, sm.state.value.balance)
        }

    // ── Surrender ─────────────────────────────────────────────────────────────

    @Test
    fun surrender_validOnFirstDecision() =
        runTest {
            // balance=800, bet=100 → surrender → balance = 800 + 50
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SEVEN)),
                        playerBets = persistentListOf(100),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(850, state.balance)
            assertEquals(GameStatus.DEALER_WON, state.status)
        }

    @Test
    fun surrender_invalidAfterHit() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TWO), card(Rank.FIVE))),
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(800, state.balance)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun surrender_noOpWhenRuleDisabled() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SEVEN)),
                        playerBets = persistentListOf(100),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        rules = GameRules(allowSurrender = false),
                    ),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(800, state.balance)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    // ── Multi-hand status & payouts ───────────────────────────────────────────

    @Test
    fun multiHand_independentPayouts() =
        runTest {
            // Hand0 TEN+TEN=20 wins vs dealer 18; Hand1 TEN+TEN+SIX=26 busts → balance += 200
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.TEN),
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TEN), card(Rank.SIX))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = hand(Rank.TEN, Rank.EIGHT),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(1000, sm.state.value.balance)
        }

    @Test
    fun multiHand_playerWonIfAnyHandWins() =
        runTest {
            // Hand0 wins, Hand1 busts
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.TEN),
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TEN), card(Rank.SIX))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = hand(Rank.TEN, Rank.EIGHT),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(GameStatus.PLAYER_WON, sm.state.value.status)
        }

    @Test
    fun multiHand_dealerWonIfAllHandsLose() =
        runTest {
            // Both hands lose to dealer 20
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.FIVE),
                                hand(Rank.NINE, Rank.FOUR),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = hand(Rank.TEN, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(GameStatus.DEALER_WON, sm.state.value.status)
        }

    @Test
    fun multiHand_pushIfAllHandsPush() =
        runTest {
            // Both hands push dealer at 19; balance = 800 + 100 + 100
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.NINE),
                                hand(Rank.NINE, Rank.TEN),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                        deck = persistentListOf(),
                    ),
                )
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance)
        }
}
