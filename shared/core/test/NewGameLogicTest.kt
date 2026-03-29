package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertEquals

class NewGameLogicTest {
    // ── Bet normalisation ──────────────────────────────────────────────────────

    @Test
    fun previousBets_truncated_whenLongerThanHandCount() {
        val state =
            NewGameLogic.createInitialState(
                balance = 1000,
                handCount = 2,
                previousBets = persistentListOf(10, 20, 30),
            )
        assertEquals(2, state.playerHands.size)
        assertEquals(10, state.playerHands[0].bet)
        assertEquals(20, state.playerHands[1].bet)
    }

    @Test
    fun previousBets_padded_whenShorterThanHandCount() {
        val state =
            NewGameLogic.createInitialState(
                balance = 1000,
                handCount = 3,
                previousBets = persistentListOf(50),
            )
        assertEquals(50, state.playerHands[0].bet)
        assertEquals(0, state.playerHands[1].bet)
        assertEquals(0, state.playerHands[2].bet)
    }

    // ── Main bet affordability ─────────────────────────────────────────────────

    @Test
    fun betsApplied_andBalanceReduced_whenAffordable() {
        val state =
            NewGameLogic.createInitialState(
                balance = 1000,
                handCount = 3,
                previousBets = persistentListOf(10, 25, 50),
            )
        assertEquals(listOf(10, 25, 50), state.playerHands.map { it.bet })
        assertEquals(1000 - 10 - 25 - 50, state.balance)
    }

    @Test
    fun betsResetToZero_whenUnaffordable() {
        val state =
            NewGameLogic.createInitialState(
                balance = 150,
                handCount = 2,
                previousBets = persistentListOf(100, 100),
            )
        assertEquals(listOf(0, 0), state.playerHands.map { it.bet })
        assertEquals(150, state.balance)
    }

    @Test
    fun lastBets_alwaysStoreNormalizedBets_regardlessOfAffordability() {
        val previousBets = persistentListOf(100, 100)
        val state =
            NewGameLogic.createInitialState(
                balance = 150,
                handCount = 2,
                previousBets = previousBets,
            )
        // finalBets are zeroed, but lastBet should mirror the normalized attempt
        assertEquals(previousBets, state.playerHands.map { it.lastBet }.toPersistentList())
    }

    // ── Side bet affordability ─────────────────────────────────────────────────

    @Test
    fun sideBetsApplied_andBalanceReduced_whenAffordable() {
        val sideBets =
            persistentMapOf(
                SideBetType.PERFECT_PAIRS to 10,
                SideBetType.TWENTY_ONE_PLUS_THREE to 25,
            )
        val state =
            NewGameLogic.createInitialState(
                balance = 1000,
                lastSideBets = sideBets,
            )
        assertEquals(10, state.sideBets[SideBetType.PERFECT_PAIRS])
        assertEquals(25, state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE])
        assertEquals(1000 - 10 - 25, state.balance)
    }

    @Test
    fun sideBetsCleared_whenUnaffordableAfterMainBets() {
        val sideBets =
            persistentMapOf(
                SideBetType.PERFECT_PAIRS to 100,
                SideBetType.TWENTY_ONE_PLUS_THREE to 100,
            )
        // 50 main bet leaves 100 remaining, but side bets total 200
        val state =
            NewGameLogic.createInitialState(
                balance = 150,
                handCount = 1,
                previousBets = persistentListOf(50),
                lastSideBets = sideBets,
            )
        assertEquals(0, state.sideBets.size)
        assertEquals(100, state.balance)
    }

    @Test
    fun lastSideBets_alwaysPreserved_regardlessOfAffordability() {
        val sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 200)
        val state =
            NewGameLogic.createInitialState(
                balance = 50,
                lastSideBets = sideBets,
            )
        // Side bet not applied, but lastSideBets preserved for next round repeat
        assertEquals(0, state.sideBets.size)
        assertEquals(sideBets, state.lastSideBets)
    }

    // ── GameState shape ────────────────────────────────────────────────────────

    @Test
    fun returnsStatus_BETTING() {
        val state = NewGameLogic.createInitialState(balance = 500)
        assertEquals(GameStatus.BETTING, state.status)
    }

    @Test
    fun activeHandIndex_isZero() {
        val state = NewGameLogic.createInitialState(balance = 500, handCount = 3)
        assertEquals(0, state.activeHandIndex)
    }

    @Test
    fun handCount_matchesParameter() {
        val state = NewGameLogic.createInitialState(balance = 500, handCount = 2)
        assertEquals(2, state.handCount)
        assertEquals(2, state.playerHands.size)
    }

    @Test
    fun rules_propagatedToState() {
        val rules = GameRules(allowSurrender = true, dealerHitsSoft17 = false)
        val state = NewGameLogic.createInitialState(balance = 500, rules = rules)
        assertEquals(rules, state.rules)
    }
}
