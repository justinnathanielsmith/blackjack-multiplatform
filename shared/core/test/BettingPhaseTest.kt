@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 1000, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 1000, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 100, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 1000, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 1000,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    )
                )
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
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 900, playerHands = persistentListOf(Hand(bet = 100)))
                )
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
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
// removed:                         currentBets = persistentListOf(100),
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    )
                )
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 1000, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
// removed:                     currentBets = persistentListOf(100),
                    playerHands =
                        kotlinx.collections.immutable.persistentListOf(
                            hand(Rank.FIVE, Rank.SIX).copy(bet = 100)
                        ),
                    handCount = 1,
                )
            val sm =
                testMachine(
                    initialState
                )
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING, balance = 1000, playerHands = persistentListOf(Hand(bet = 0)))
                )
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
    fun selectHandCount_addingSeatsDoesNotPreCharge() =
        runTest {
            // Under the new model, adding a seat is free — new seat starts at 0.
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        handCount = 1
                    )
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(3))
                val state = awaitItem()
                assertEquals(900, state.balance) // unchanged — no pre-charge
                assertEquals(3, state.handCount)
                assertEquals(3, state.playerHands.size)
                assertEquals(100, state.playerHands[0].bet)
                assertEquals(0, state.playerHands[1].bet)
                assertEquals(0, state.playerHands[2].bet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_refundsBalanceWhenReducingHands() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 700,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100)),
                        handCount = 3,
                    )
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.SelectHandCount(2))
                val state = awaitItem()
                assertEquals(800, state.balance) // refunded seat-2's $100
                assertEquals(2, state.handCount)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun selectHandCount_noBalanceChangeWhenNoBet() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0)),
                        handCount = 1
                    )
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
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 800,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100)),
                        handCount = 2,
                    )
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.Deal)
                cancelAndIgnoreRemainingEvents()
            }
            val state = sm.state.value
            assertEquals(2, state.playerHands.size)
            assertEquals(listOf(100, 100), state.playerHands.map { it.bet })
        }

    @Test
    fun deal_doesNotDeductBalanceForMultiHand() =
        runTest {
            // 3 hands, bets=[100,100,100]: all 3 already paid via PlaceBet → balance=700
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 700,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100)),
                        handCount = 3,
                    )
                )
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.Deal)
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(700, sm.state.value.balance)
        }

    @Test
    fun placeBet_onSeat_deductsSeatAmountOnly() =
        runTest {
            // With 3 seats, placing $10 on seat 0 costs $10 — not $30
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 0), Hand(bet = 0)),
                    handCount = 3,
                )
            val sm = testMachine(initialState)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(10, seatIndex = 0))
                val state = awaitItem()
                assertEquals(990, state.balance)
                assertEquals(10, state.playerHands[0].bet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun deal_rejectedIfAnySeatHasNoBet() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 900,
                    playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 0)),
                    handCount = 2,
                    deck = deckOf(Rank.TEN, Rank.NINE, Rank.TEN, Rank.NINE),
                )
            val sm = testMachine(initialState)
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.Deal)
                expectNoEvents() // deal rejected — seat 1 is unfunded
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resetBet_refundsAllSeats() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 700,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100)),
                        handCount = 3,
                    )
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
    fun newGame_resetsToOneSeat() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.DEALER_WON,
                        balance = 800,
                        playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 0), Hand(bet = 0)),
                        handCount = 3,
                    )
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
            val sm =
                testMachine()
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
            val sm =
                testMachine()
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
            val sm =
                testMachine(
                    GameState(status = GameStatus.BETTING)
                )
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
