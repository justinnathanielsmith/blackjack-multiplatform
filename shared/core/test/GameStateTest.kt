package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlin.test.Test
import kotlin.test.assertEquals

class GameStateTest {
    @Test
    fun totalBet_calculatesCorrectly_inBettingPhase() {
        val state = GameState(
            status = GameStatus.BETTING,
            currentBet = 100,
            handCount = 3,
            sideBets = persistentMapOf(
                SideBetType.PERFECT_PAIRS to 50,
                SideBetType.TWENTY_ONE_PLUS_THREE to 25
            )
        )
        // (100 * 3) + 50 + 25 = 375
        assertEquals(375, state.totalBet)
    }

    @Test
    fun totalBet_calculatesCorrectly_inPlayingPhase() {
        val state = GameState(
            status = GameStatus.PLAYING,
            playerBets = persistentListOf(100, 200),
            sideBets = persistentMapOf(
                SideBetType.PERFECT_PAIRS to 50
            ),
            insuranceBet = 50
        )
        // (100 + 200) + 50 + 50 = 400
        assertEquals(400, state.totalBet)
    }

    @Test
    fun totalBet_isZero_whenNoBets() {
        val state = GameState(status = GameStatus.BETTING)
        assertEquals(0, state.totalBet)
    }
}
