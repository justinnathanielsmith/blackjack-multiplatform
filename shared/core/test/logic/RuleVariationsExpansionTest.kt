@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.util.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Focuses on business logic variations and edge cases for rule configurations
 * and betting actions that were previously under-tested.
 */
class RuleVariationsExpansionTest {
    // ── splitOnValueOnly tests ────────────────────────────────────────────────

    @Test
    fun splitOnValueOnly_disabled_blocksRankMismatch() {
        // King and Queen have same value (10) but different ranks.
        // By default (splitOnValueOnly=false), this should not be splittable.
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.KING, Rank.QUEEN).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING,
                rules = GameRules(splitOnValueOnly = false)
            )
        assertFalse(state.canSplit, "King and Queen should NOT be splittable when splitOnValueOnly is false")
    }

    @Test
    fun splitOnValueOnly_disabled_allowsRankMatch() {
        // Two Kings have same rank. Should always be splittable.
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.KING, Rank.KING).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING,
                rules = GameRules(splitOnValueOnly = false)
            )
        assertTrue(state.canSplit, "Two Kings should be splittable even when splitOnValueOnly is false")
    }

    @Test
    fun splitOnValueOnly_enabled_allowsRankMismatch() {
        // King and Queen should be splittable when splitOnValueOnly is true.
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.KING, Rank.QUEEN).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING,
                rules = GameRules(splitOnValueOnly = true)
            )
        assertTrue(state.canSplit, "King and Queen SHOULD be splittable when splitOnValueOnly is true")
    }

    @Test
    fun splitOnValueOnly_enabled_allowsMixedTens() {
        // 10 and Jack should be splittable when splitOnValueOnly is true.
        val state =
            GameState(
                playerHands = persistentListOf(hand(Rank.TEN, Rank.JACK).copy(bet = 100)),
                balance = 1000,
                status = GameStatus.PLAYING,
                rules = GameRules(splitOnValueOnly = true)
            )
        assertTrue(state.canSplit, "10 and Jack SHOULD be splittable when splitOnValueOnly is true")
    }

    // ── ResetSeatBet tests ────────────────────────────────────────────────────

    @Test
    fun resetSeatBet_refundsSpecificSeat() =
        runTest {
            // 3 hands, bets=[100, 200, 300]. Resetting seat 1 (200).
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 400,
                    playerHands =
                        persistentListOf(
                            Hand(bet = 100),
                            Hand(bet = 200),
                            Hand(bet = 300)
                        ),
                    handCount = 3
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.ResetSeatBet(1))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(600, state.balance, "Balance should increase by seat 1's bet (400 + 200)")
            assertEquals(0, state.playerHands[1].bet, "Seat 1's bet should be cleared")
            assertEquals(100, state.playerHands[0].bet, "Seat 0's bet should be untouched")
            assertEquals(300, state.playerHands[2].bet, "Seat 2's bet should be untouched")
        }

    @Test
    fun resetSeatBet_invalidIndex_isIgnored() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 900,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.ResetSeatBet(1)) // Out of bounds
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value, "Action with invalid seat index should be ignored")
        }

    @Test
    fun resetSeatBet_wrongStatus_isIgnored() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    handCount = 1,
                    dealerHand = hand(Rank.TEN, Rank.SEVEN)
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.ResetSeatBet(0))
            advanceUntilIdle()

            assertEquals(initialState, sm.state.value, "ResetSeatBet should be ignored outside of BETTING status")
        }
}
