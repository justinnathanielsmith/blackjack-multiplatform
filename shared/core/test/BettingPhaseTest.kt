@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

class BettingPhaseTest {
    // ── PlaceBet ──────────────────────────────────────────────────────────────

    @Test
    fun placeBet_decreasesBalanceAndIncreasesCurrentBet() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(100))
                val state = awaitItem()
                assertEquals(900, state.balance)
                assertEquals(100, state.currentBet)
                assertEquals(GameStatus.BETTING, state.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_multipleChipsAccumulate() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(50))
                awaitItem() // state after first bet
                sm.dispatch(GameAction.PlaceBet(100))
                val state = awaitItem()
                assertEquals(850, state.balance)
                assertEquals(150, state.currentBet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_rejectedWhenAmountExceedsBalance() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 100, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(200))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_rejectedWhenAmountZeroOrNegative() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(0))
                expectNoEvents()
                sm.dispatch(GameAction.PlaceBet(-50))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm =
                BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.PLAYING, balance = 1000, currentBet = 100))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(50))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── ResetBet ──────────────────────────────────────────────────────────────

    @Test
    fun resetBet_restoresBalanceAndClearsCurrentBet() =
        runTest {
            val sm =
                BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.ResetBet)
                val state = awaitItem()
                assertEquals(1000, state.balance)
                assertEquals(0, state.currentBet)
                assertEquals(GameStatus.BETTING, state.status)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm =
                BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.PLAYING, balance = 900, currentBet = 100))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.ResetBet)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── SelectHandCount ───────────────────────────────────────────────────────

    @Test
    fun selectHandCount_updatesHandCount() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(3))
                val state = awaitItem()
                assertEquals(3, state.handCount)
                cancelAndIgnoreRemainingEvents()
            }
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
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState, isTest = true)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(2))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_ignoresInvalidValues() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(0))
                expectNoEvents()
                sm.dispatch(GameAction.SelectHandCount(4))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(1, sm.state.value.handCount)
        }

    @Test
    fun selectHandCount_adjustsBalanceWhenBetPlaced() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100, handCount = 1)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(3))
                val state = awaitItem()
                assertEquals(700, state.balance)
                assertEquals(3, state.handCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_refundsBalanceWhenReducingHands() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(2))
                val state = awaitItem()
                assertEquals(800, state.balance)
                assertEquals(2, state.handCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_rejectedIfInsufficientBalance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 50, currentBet = 100, handCount = 1)
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState, isTest = true)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(2))
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_noBalanceChangeWhenNoBet() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 1)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(3))
                val state = awaitItem()
                assertEquals(1000, state.balance)
                assertEquals(3, state.handCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── Multi-hand bet mechanics ───────────────────────────────────────────────

    @Test
    fun deal_createsTwoHandsWhenHandCount2() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 100, handCount = 2)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.Deal)
                cancelAndIgnoreRemainingEvents()
            }
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
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.Deal)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(700, sm.state.value.balance)
        }

    @Test
    fun placeBet_rejectedWhenTotalCostExceedsBalance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 25, currentBet = 0, handCount = 3)
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), initialState, isTest = true)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(10)) // total cost = 30 > 25
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_deductsTotalCostForAllHands() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 3)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(10))
                val state = awaitItem()
                assertEquals(970, state.balance)
                assertEquals(10, state.currentBet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetBet_refundsTotalCost() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.ResetBet)
                val state = awaitItem()
                assertEquals(1000, state.balance)
                assertEquals(0, state.currentBet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── NewGame ───────────────────────────────────────────────────────────────

    @Test
    fun newGame_resetsHandCountTo1() =
        runTest {
            val sm =
                BlackjackStateMachine(
                    kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)),
                    GameState(status = GameStatus.DEALER_WON, balance = 800, currentBet = 0, handCount = 3)
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.NewGame())
                val state = awaitItem()
                assertEquals(1, state.handCount)
                assertEquals(1, state.playerHands.size)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun newGame_usesRulesDeckCount() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), isTest = true)
            sm.dispatch(GameAction.NewGame(rules = GameRules(deckCount = 2)))
            sm.dispatch(GameAction.PlaceBet(100))
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()
            
            // 2 decks = 104 cards; dealt 4 = 100 remaining
            assertEquals(100, sm.state.value.deck.size)
        }

    @Test
    fun newGame_defaultDeckCountIs6() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), isTest = true)
            sm.dispatch(GameAction.NewGame())
            sm.dispatch(GameAction.PlaceBet(100))
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()
            
            // 6 decks = 312 cards; dealt 4 = 308 remaining
            assertEquals(308, sm.state.value.deck.size)
        }

    @Test
    fun updateRules_updatesGameRules() =
        runTest {
            val sm = BlackjackStateMachine(kotlinx.coroutines.CoroutineScope(backgroundScope.coroutineContext + kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)), GameState(status = GameStatus.BETTING))
            val newRules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.UpdateRules(newRules))
                val state = awaitItem()
                assertEquals(newRules, state.rules)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
