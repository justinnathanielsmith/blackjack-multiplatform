@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.BlackjackPayout
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.assertNoTransition
import io.github.smithjustinn.blackjack.util.assertTransition
import io.github.smithjustinn.blackjack.util.bettingReadyToDeal
import io.github.smithjustinn.blackjack.util.bettingState
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.defaultPlayingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BettingPhaseTest {
    // ── PlaceBet ──────────────────────────────────────────────────────────────

    @Test
    fun placeBet_decreasesBalanceAndIncreasesCurrentBet() =
        runTest {
            val sm = testMachine(bettingState())
            sm.assertTransition(GameAction.PlaceBet(100)) { state ->
                assertEquals(900, state.balance)
                assertEquals(100, state.currentBet)
                assertEquals(GameStatus.BETTING, state.status)
            }
        }

    @Test
    fun placeBet_multipleChipsAccumulate() =
        runTest {
            val sm = testMachine(bettingState())
            sm.state.test {
                awaitItem() // initial state
                sm.dispatch(GameAction.PlaceBet(50))
                awaitItem()
                sm.dispatch(GameAction.PlaceBet(100))
                val state = awaitItem()
                assertEquals(850, state.balance)
                assertEquals(150, state.currentBet)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun placeBet_rejectedForInvalidAmounts() =
        runTest {
            // 0 (zero), -50 (negative), 1500 (exceeds balance)
            listOf(0, -50, 1500).forEach { amount ->
                val sm = testMachine(bettingState())
                sm.assertNoTransition(GameAction.PlaceBet(amount))
            }
        }

    @Test
    fun placeBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm = testMachine(defaultPlayingState())
            sm.assertNoTransition(GameAction.PlaceBet(50))
        }

    @Test
    fun resetSideBet_restoresBalanceAndClearsSpecificSideBet() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 900).copy(
                        sideBets =
                            persistentMapOf(
                                SideBetType.PERFECT_PAIRS to 50,
                                SideBetType.TWENTY_ONE_PLUS_THREE to 50,
                            )
                    )
                )
            sm.assertTransition(GameAction.ResetSideBet(SideBetType.PERFECT_PAIRS)) { state ->
                assertEquals(950, state.balance)
                assertNull(state.sideBets[SideBetType.PERFECT_PAIRS])
                assertEquals(50, state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE])
            }
        }

    // ── ResetBet ──────────────────────────────────────────────────────────────

    @Test
    fun resetBet_restoresBalanceAndClearsCurrentBet() =
        runTest {
            val sm = testMachine(bettingReadyToDeal())
            sm.assertTransition(GameAction.ResetBet) { state ->
                assertEquals(1000, state.balance)
                assertEquals(0, state.currentBet)
                assertEquals(GameStatus.BETTING, state.status)
            }
        }

    @Test
    fun resetBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val sm = testMachine(defaultPlayingState())
            sm.assertNoTransition(GameAction.ResetBet)
        }

    // ── SelectHandCount ───────────────────────────────────────────────────────

    @Test
    fun selectHandCount_updatesHandCount() =
        runTest {
            val sm = testMachine(bettingState())
            sm.assertTransition(GameAction.SelectHandCount(3)) { state ->
                assertEquals(3, state.handCount)
            }
        }

    @Test
    fun selectHandCount_ignoredOutsideBetting() =
        runTest {
            val sm = testMachine(defaultPlayingState())
            sm.assertNoTransition(GameAction.SelectHandCount(2))
        }

    @Test
    fun selectHandCount_ignoresInvalidValues() =
        runTest {
            listOf(0, 4).forEach { count ->
                val sm = testMachine(bettingState())
                sm.assertNoTransition(GameAction.SelectHandCount(count))
                assertEquals(1, sm.state.value.handCount)
            }
        }

    @Test
    fun selectHandCount_addingSeatsDoesNotPreCharge() =
        runTest {
            // Under the new model, adding a seat is free — new seat starts at 0.
            val sm = testMachine(bettingReadyToDeal(bet = 100, balance = 900))
            sm.assertTransition(GameAction.SelectHandCount(3)) { state ->
                assertEquals(900, state.balance) // unchanged — no pre-charge
                assertEquals(3, state.handCount)
                assertEquals(3, state.playerHands.size)
                assertEquals(100, state.playerHands[0].bet)
                assertEquals(0, state.playerHands[1].bet)
                assertEquals(0, state.playerHands[2].bet)
            }
        }

    @Test
    fun selectHandCount_refundsBalanceWhenReducingHands() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 700, handCount = 3).copy(
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100))
                    )
                )
            sm.assertTransition(GameAction.SelectHandCount(2)) { state ->
                assertEquals(800, state.balance) // refunded seat-2's $100
                assertEquals(2, state.handCount)
            }
        }

    @Test
    fun selectHandCount_noBalanceChangeWhenNoBet() =
        runTest {
            val sm = testMachine(bettingState())
            sm.assertTransition(GameAction.SelectHandCount(3)) { state ->
                assertEquals(1000, state.balance)
                assertEquals(3, state.handCount)
            }
        }

    // ── Multi-hand bet mechanics ───────────────────────────────────────────────

    @Test
    fun deal_createsTwoHandsWhenHandCount2() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 800, handCount = 2).copy(
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100))
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
                    bettingState(balance = 700, handCount = 3).copy(
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100)),
                        deck = deckOf(Rank.TWO, Rank.TWO, Rank.TWO, Rank.TWO, Rank.TWO, Rank.TWO, Rank.TWO, Rank.TWO),
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
            val sm = testMachine(bettingState(balance = 1000, handCount = 3))
            sm.assertTransition(GameAction.PlaceBet(10, seatIndex = 0)) { state ->
                assertEquals(990, state.balance)
                assertEquals(10, state.playerHands[0].bet)
            }
        }

    @Test
    fun deal_rejectedIfAnySeatHasNoBet() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 900, handCount = 2).copy(
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 0)),
                        deck = deckOf(Rank.TEN, Rank.NINE, Rank.TEN, Rank.NINE),
                    )
                )
            sm.assertNoTransition(GameAction.Deal)
        }

    @Test
    fun resetBet_refundsAllSeats() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 700, handCount = 3).copy(
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 100), Hand(bet = 100))
                    )
                )
            sm.assertTransition(GameAction.ResetBet) { state ->
                assertEquals(1000, state.balance)
                assertEquals(0, state.currentBet)
            }
        }

    // ── NewGame ───────────────────────────────────────────────────────────────

    @Test
    fun newGame_resetsToOneSeat() =
        runTest {
            val sm =
                testMachine(
                    bettingState(balance = 800, handCount = 3).copy(status = GameStatus.DEALER_WON)
                )
            sm.assertTransition(GameAction.NewGame()) { state ->
                assertEquals(1, state.handCount)
                assertEquals(1, state.playerHands.size)
            }
        }

    @Test
    fun newGame_usesRulesDeckCount() =
        runTest {
            val sm = testMachine()
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
            val sm = testMachine()
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
            val sm = testMachine(bettingState())
            val newRules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE)
            sm.assertTransition(GameAction.UpdateRules(newRules)) { state ->
                assertEquals(newRules, state.rules)
            }
        }
}
