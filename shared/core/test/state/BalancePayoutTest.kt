@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.BlackjackPayout
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.util.card
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.multiHandPlayingState
import io.github.smithjustinn.blackjack.util.testMachine
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
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100)),
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
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100)),
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
    fun blackjackPayout_3to2_oddBet_truncates() =
        runTest {
            // Bet=3, 3:2 BJ payout: profit = (3 * 3) / 2 = 4 (not 4.5)
            // balance=997, bet=3 → payout = 3 + 4 = 7 → balance=1004
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 997,
                        playerHands = persistentListOf(Hand(bet = 3)),
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
            assertEquals(1004, state.balance) // 997 + 7 (3 + floor(3*3/2)=4)
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
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        persistentListOf(
                            hand(Rank.ACE, Rank.TEN).copy(bet = 100),
                            hand(Rank.TEN, Rank.TEN).copy(bet = 100),
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

            assertEquals(1250, sm.state.value.balance)
        }

    // ── Surrender ─────────────────────────────────────────────────────────────

    @Test
    fun surrender_validOnFirstDecision() =
        runTest {
            // balance=800, bet=100 → surrender → balance = 800 + 50
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SEVEN).copy(bet = 100)),
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TWO), card(Rank.FIVE)), bet = 100),
                            ),
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SEVEN).copy(bet = 100)),
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

    @Test
    fun surrender_multiHand_firstHandAdvancesToSecondHand() =
        runTest {
            // Hand0 surrenders (bet=100, refund=50) → advance to Hand1
            // Hand1 (TEN+TEN=20) stands vs dealer 18 → payout=200
            // balance=800 → after surrender: 850 → after Hand1 win: 1050
            val sm =
                testMachine(
                    multiHandPlayingState(
                        balance = 800,
                        hands = listOf(hand(Rank.TEN, Rank.SEVEN), hand(Rank.TEN, Rank.TEN)),
                        bets = listOf(100, 100),
                        activeHandIndex = 0,
                        dealerHand = dealerHand(Rank.TEN, Rank.EIGHT),
                    ).copy(rules = GameRules(allowSurrender = true)),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            // Hand0 surrendered: refund applied, now on Hand1
            assertEquals(GameStatus.PLAYING, sm.state.value.status)
            assertEquals(1, sm.state.value.activeHandIndex)
            assertEquals(850, sm.state.value.balance)

            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(GameStatus.PLAYER_WON, sm.state.value.status)
            assertEquals(1050, sm.state.value.balance)
        }

    @Test
    fun surrender_multiHand_lastHandGoesToDealerTurn() =
        runTest {
            // Hand0 already played (TEN+TEN=20); Hand1 surrenders (bet=100, refund=50)
            // Dealer TEN+EIGHT=18. Hand0 wins (payout=200); Hand1 surrendered (payout=0).
            // balance=800 → after surrender: 850 → after Hand0 win: 850 + 200 = 1050
            val sm =
                testMachine(
                    multiHandPlayingState(
                        balance = 800,
                        hands = listOf(hand(Rank.TEN, Rank.TEN), hand(Rank.TEN, Rank.SEVEN)),
                        bets = listOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = dealerHand(Rank.TEN, Rank.EIGHT),
                    ).copy(rules = GameRules(allowSurrender = true)),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1050, state.balance)
        }

    @Test
    fun surrender_multiHand_allHandsSurrendered_dealerWon() =
        runTest {
            // Hand0 surrenders, Hand1 surrenders → no non-surrendered hands → DEALER_WON
            // balance=800, bets=[100,100] → surrender Hand0: +50 → advance to Hand1 → surrender Hand1: +50
            // → dealer turn → payout=0 → balance=900, status=DEALER_WON
            val sm =
                testMachine(
                    multiHandPlayingState(
                        balance = 800,
                        hands = listOf(hand(Rank.TEN, Rank.SEVEN), hand(Rank.TEN, Rank.SEVEN)),
                        bets = listOf(100, 100),
                        activeHandIndex = 0,
                        dealerHand = dealerHand(Rank.TEN, Rank.EIGHT),
                    ).copy(rules = GameRules(allowSurrender = true)),
                )
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()
            sm.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(900, state.balance)
        }

    // ── Multi-hand status & payouts ───────────────────────────────────────────

    @Test
    fun multiHand_independentPayouts() =
        runTest {
            // Hand0 TEN+TEN=20 wins vs dealer 18; Hand1 TEN+TEN+SIX=26 busts → balance += 200
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TEN), card(Rank.SIX)), bet = 100),
                            ),
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.TEN).copy(bet = 100),
                                Hand(persistentListOf(card(Rank.TEN), card(Rank.TEN), card(Rank.SIX)), bet = 100),
                            ),
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.FIVE).copy(bet = 100),
                                hand(Rank.NINE, Rank.FOUR).copy(bet = 100),
                            ),
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
// removed:                         currentBets = persistentListOf(100),
                        playerHands =
                            persistentListOf(
                                hand(Rank.TEN, Rank.NINE).copy(bet = 100),
                                hand(Rank.NINE, Rank.TEN).copy(bet = 100),
                            ),
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
