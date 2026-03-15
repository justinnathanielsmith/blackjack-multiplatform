@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingPhaseTest {
    // ── PlaceBet ──────────────────────────────────────────────────────────────

    @Test
    fun placeBet_decreasesBalanceAndIncreasesCurrentBet() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun placeBet_multipleChipsAccumulate() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(850, state.balance)
            assertEquals(150, state.currentBet)
        }

    @Test
    fun placeBet_rejectedWhenAmountExceedsBalance() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 100, currentBet = 0))
            sm.dispatch(GameAction.PlaceBet(200))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(100, state.balance)
            assertEquals(0, state.currentBet)
        }

    @Test
    fun placeBet_rejectedWhenAmountZeroOrNegative() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.PlaceBet(0))
            advanceUntilIdle()
            assertEquals(1000, sm.state.value.balance)
            assertEquals(0, sm.state.value.currentBet)

            sm.dispatch(GameAction.PlaceBet(-50))
            advanceUntilIdle()
            assertEquals(1000, sm.state.value.balance)
            assertEquals(0, sm.state.value.currentBet)
        }

    @Test
    fun placeBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 1000, currentBet = 100))
            sm.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertEquals(100, state.currentBet)
        }

    // ── ResetBet ──────────────────────────────────────────────────────────────

    @Test
    fun resetBet_restoresBalanceAndClearsCurrentBet() =
        runTest {
            val sm =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100))
            sm.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertEquals(0, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun resetBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 900, currentBet = 100))
            sm.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
        }

    // ── SelectHandCount ───────────────────────────────────────────────────────

    @Test
    fun selectHandCount_updatesHandCount() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            assertEquals(3, sm.state.value.handCount)
        }

    @Test
    fun selectHandCount_ignoredOutsideBetting() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        kotlinx.collections.immutable.persistentListOf(
                            hand(Rank.FIVE, Rank.SIX)
                        ),
                    playerBets = kotlinx.collections.immutable.persistentListOf(100),
                    handCount = 1,
                )
            val sm = BlackjackStateMachine(this, initialState)
            sm.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun selectHandCount_ignoresInvalidValues() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.dispatch(GameAction.SelectHandCount(0))
            advanceUntilIdle()
            assertEquals(1, sm.state.value.handCount)

            sm.dispatch(GameAction.SelectHandCount(4))
            advanceUntilIdle()
            assertEquals(1, sm.state.value.handCount)
        }

    @Test
    fun selectHandCount_adjustsBalanceWhenBetPlaced() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100, handCount = 1)
                )
            sm.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(700, state.balance)
            assertEquals(3, state.handCount)
        }

    @Test
    fun selectHandCount_refundsBalanceWhenReducingHands() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(800, state.balance)
            assertEquals(2, state.handCount)
        }

    @Test
    fun selectHandCount_rejectedIfInsufficientBalance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 50, currentBet = 100, handCount = 1)
            val sm = BlackjackStateMachine(this, initialState)
            sm.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun selectHandCount_noBalanceChangeWhenNoBet() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 1)
                )
            sm.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertEquals(3, state.handCount)
        }

    // ── Multi-hand bet mechanics ───────────────────────────────────────────────

    @Test
    fun deal_createsTwoHandsWhenHandCount2() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 100, handCount = 2)
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(2, state.playerHands.size)
            assertEquals(kotlinx.collections.immutable.persistentListOf(100, 100), state.playerBets)
        }

    @Test
    fun deal_doesNotDeductBalanceForMultiHand() =
        runTest {
            // 3 hands, bet=100: all 3 hands already paid via PlaceBet → balance=700
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            assertEquals(700, sm.state.value.balance)
        }

    @Test
    fun placeBet_rejectedWhenTotalCostExceedsBalance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 25, currentBet = 0, handCount = 3)
            val sm = BlackjackStateMachine(this, initialState)
            sm.dispatch(GameAction.PlaceBet(10)) // total cost = 30 > 25
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value)
        }

    @Test
    fun placeBet_deductsTotalCostForAllHands() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 3)
                )
            sm.dispatch(GameAction.PlaceBet(10))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(970, state.balance)
            assertEquals(10, state.currentBet)
        }

    @Test
    fun resetBet_refundsTotalCost() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1000, state.balance)
            assertEquals(0, state.currentBet)
        }

    // ── NewGame ───────────────────────────────────────────────────────────────

    @Test
    fun newGame_resetsHandCountTo1() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.DEALER_WON, balance = 800, currentBet = 0, handCount = 3)
                )
            sm.dispatch(GameAction.NewGame())
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(1, state.handCount)
            assertEquals(1, state.playerHands.size)
        }

    @Test
    fun newGame_usesRulesDeckCount() =
        runTest {
            val sm = BlackjackStateMachine(this)
            sm.dispatch(GameAction.NewGame(rules = GameRules(deckCount = 2)))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100))
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // 2 decks = 104 cards; dealt 4 = 100 remaining
            assertEquals(100, sm.state.value.deck.size)
        }

    @Test
    fun newGame_defaultDeckCountIs6() =
        runTest {
            val sm = BlackjackStateMachine(this)
            sm.dispatch(GameAction.NewGame())
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(100))
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // 6 decks = 312 cards; dealt 4 = 308 remaining
            assertEquals(308, sm.state.value.deck.size)
        }

    @Test
    fun updateRules_updatesGameRules() =
        runTest {
            val sm = BlackjackStateMachine(this, GameState(status = GameStatus.BETTING))
            val newRules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            assertEquals(newRules, sm.state.value.rules)
        }
}
