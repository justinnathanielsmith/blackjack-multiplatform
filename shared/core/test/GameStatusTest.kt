package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameStatusTest {
    @Test
    fun isTerminal_returnsTrue_forTerminalStates() {
        assertTrue(GameStatus.PLAYER_WON.isTerminal())
        assertTrue(GameStatus.DEALER_WON.isTerminal())
        assertTrue(GameStatus.PUSH.isTerminal())
    }

    @Test
    fun isTerminal_returnsFalse_forNonTerminalStates() {
        assertFalse(GameStatus.BETTING.isTerminal())
        assertFalse(GameStatus.DEALING.isTerminal())
        assertFalse(GameStatus.IDLE.isTerminal())
        assertFalse(GameStatus.PLAYING.isTerminal())
        assertFalse(GameStatus.INSURANCE_OFFERED.isTerminal())
        assertFalse(GameStatus.DEALER_TURN.isTerminal())
    }

    @Test
    fun isStatusVisible_returnsTrue_forVisibleStates() {
        assertTrue(GameStatus.DEALING.isStatusVisible())
        assertTrue(GameStatus.DEALER_TURN.isStatusVisible())
        assertTrue(GameStatus.PLAYER_WON.isStatusVisible())
        assertTrue(GameStatus.DEALER_WON.isStatusVisible())
        assertTrue(GameStatus.PUSH.isStatusVisible())
    }

    @Test
    fun isStatusVisible_returnsFalse_forHiddenStates() {
        assertFalse(GameStatus.BETTING.isStatusVisible())
        assertFalse(GameStatus.IDLE.isStatusVisible())
        assertFalse(GameStatus.PLAYING.isStatusVisible())
        assertFalse(GameStatus.INSURANCE_OFFERED.isStatusVisible())
    }
}
