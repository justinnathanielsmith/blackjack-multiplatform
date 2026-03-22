@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PerSeatBettingTest {
    // ── PlaceBet per-seat ─────────────────────────────────────────────────────

    @Test
    fun placeBet_seat0_onlyDeductsSeatAmount() =
        runTest {
            // 3 active seats; placing $100 on seat 0 should cost exactly $100, not $300
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 0), Hand(bet = 0)),
                        handCount = 3,
                    )
                )
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 0))
            advanceUntilIdle()
            assertEquals(900, sm.state.value.balance)
            assertEquals(100, sm.state.value.playerHands[0].bet)
            assertEquals(0, sm.state.value.playerHands[1].bet)
            assertEquals(0, sm.state.value.playerHands[2].bet)
        }

    @Test
    fun placeBet_seat1_independentOfSeat0() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 50), Hand(bet = 0)),
                        handCount = 2,
                    )
                )
            sm.dispatch(GameAction.PlaceBet(75, seatIndex = 1))
            advanceUntilIdle()
            assertEquals(925, sm.state.value.balance)
            assertEquals(50, sm.state.value.playerHands[0].bet)
            assertEquals(75, sm.state.value.playerHands[1].bet)
        }

    @Test
    fun placeBet_allSeats_totalMatchesSumOfBets() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 0), Hand(bet = 0)),
                        handCount = 3,
                    )
                )
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 0))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(50, seatIndex = 1))
            advanceUntilIdle()
            sm.dispatch(GameAction.PlaceBet(200, seatIndex = 2))
            advanceUntilIdle()
            assertEquals(650, sm.state.value.balance)
            assertEquals(
                350,
                sm.state.value.playerHands
                    .fold(0) { acc, hand -> acc + hand.bet }
            )
        }

    @Test
    fun placeBet_rejectedForInvalidSeatIndex() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 0)),
                        handCount = 1,
                    )
                )
            sm.dispatch(GameAction.PlaceBet(100, seatIndex = 1)) // only seat 0 exists
            advanceUntilIdle()
            assertEquals(1000, sm.state.value.balance) // no change
        }

    // ── ResetSeatBet ──────────────────────────────────────────────────────────

    @Test
    fun resetSeatBet_refundsOnlyThatSeat() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 750,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 150)),
                        handCount = 2,
                    )
                )
            sm.dispatch(GameAction.ResetSeatBet(seatIndex = 1))
            advanceUntilIdle()
            assertEquals(900, sm.state.value.balance)
            assertEquals(100, sm.state.value.playerHands[0].bet) // untouched
            assertEquals(0, sm.state.value.playerHands[1].bet)
        }

    @Test
    fun resetSeatBet_ignoredForInvalidSeatIndex() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        handCount = 1,
                    )
                )
            sm.dispatch(GameAction.ResetSeatBet(seatIndex = 2))
            advanceUntilIdle()
            assertEquals(900, sm.state.value.balance)
            assertEquals(100, sm.state.value.playerHands[0].bet)
        }

    // ── ResetBet (all seats) ──────────────────────────────────────────────────

    @Test
    fun resetAllBets_refundsAllSeats() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 650,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 150), Hand(bet = 100)),
                        handCount = 3,
                    )
                )
            sm.dispatch(GameAction.ResetBet)
            advanceUntilIdle()
            assertEquals(1000, sm.state.value.balance)
            assertTrue(
                sm.state.value.playerHands
                    .all { it.bet == 0 }
            )
        }

    // ── SelectHandCount ───────────────────────────────────────────────────────

    @Test
    fun selectHandCount_addSeat_noPreCharge() =
        runTest {
            // Adding a seat should NOT pre-charge the balance — new seat starts at 0
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        handCount = 1,
                    )
                )
            sm.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()
            assertEquals(900, sm.state.value.balance) // unchanged
            assertEquals(3, sm.state.value.handCount)
            assertEquals(3, sm.state.value.playerHands.size)
            assertEquals(100, sm.state.value.playerHands[0].bet)
            assertEquals(0, sm.state.value.playerHands[1].bet)
            assertEquals(0, sm.state.value.playerHands[2].bet)
        }

    @Test
    fun selectHandCount_removeSeat_refundsSeatBet() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 650,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 150), Hand(bet = 100)),
                        handCount = 3,
                    )
                )
            sm.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()
            assertEquals(750, sm.state.value.balance) // refunded seat 2's $100
            assertEquals(2, sm.state.value.handCount)
            assertEquals(2, sm.state.value.playerHands.size)
            assertEquals(100, sm.state.value.playerHands[0].bet)
            assertEquals(150, sm.state.value.playerHands[1].bet)
        }

    @Test
    fun selectHandCount_removeSeat_noRefundIfSeatWasEmpty() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 0)),
                        handCount = 2,
                    )
                )
            sm.dispatch(GameAction.SelectHandCount(1))
            advanceUntilIdle()
            assertEquals(900, sm.state.value.balance) // no change, seat 1 had 0
            assertEquals(1, sm.state.value.handCount)
        }

    // ── Deal guard ────────────────────────────────────────────────────────────

    @Test
    fun handleDeal_rejectedIfAnySeatHasNoBet() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 900,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 0)),
                        handCount = 2,
                        deck = deckOf(Rank.TEN, Rank.NINE, Rank.TEN, Rank.NINE),
                    )
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()
            assertEquals(GameStatus.BETTING, sm.state.value.status) // not dealt
        }

    @Test
    fun handleDeal_playerBets_matchCurrentBets() =
        runTest {
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 650,
                        playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 150), Hand(bet = 100)),
                        handCount = 3,
                        deck =
                            deckOf(
                                Rank.TEN,
                                Rank.NINE,
                                Rank.EIGHT,
                                Rank.TEN,
                                Rank.NINE,
                                Rank.EIGHT,
                                Rank.TEN,
                                Rank.SEVEN,
                            ),
                    )
                )
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()
            assertEquals(
                listOf(100, 150, 100),
                sm.state.value.playerHands
                    .map { it.bet }
            )
        }
}
