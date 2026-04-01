package io.github.smithjustinn.blackjack

import persistentListOf
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BettingPredicatesTest {
    // ── canDeal ───────────────────────────────────────────────────────────────

    @Test
    fun canDeal_returnsFalse_whenNoHands() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf()
            )
        assertFalse(state.canDeal())
    }

    @Test
    fun canDeal_returnsFalse_whenSomeHandsHaveNoBet() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 0))
            )
        assertFalse(state.canDeal())
    }

    @Test
    fun canDeal_returnsFalse_whenAllHandsHaveNoBet() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 0))
            )
        assertFalse(state.canDeal())
    }

    @Test
    fun canDeal_returnsTrue_whenAllHandsHaveBets() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 50), Hand(bet = 100))
            )
        assertTrue(state.canDeal())
    }

    @Test
    fun canDeal_returnsTrue_singleHandWithBet() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 25))
            )
        assertTrue(state.canDeal())
    }

    // ── canResetBet ───────────────────────────────────────────────────────────

    @Test
    fun canResetBet_returnsFalse_whenAllHandsHaveNoBet() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 0))
            )
        assertFalse(state.canResetBet())
    }

    @Test
    fun canResetBet_returnsTrue_whenAtLeastOneHandHasBet() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 0), Hand(bet = 50))
            )
        assertTrue(state.canResetBet())
    }

    @Test
    fun canResetBet_returnsTrue_whenAllHandsHaveBets() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf(Hand(bet = 100), Hand(bet = 200))
            )
        assertTrue(state.canResetBet())
    }

    @Test
    fun canResetBet_returnsFalse_whenNoHands() {
        val state =
            GameState(
                status = GameStatus.BETTING,
                playerHands = persistentListOf()
            )
        assertFalse(state.canResetBet())
    }
}
