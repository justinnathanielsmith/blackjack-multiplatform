package io.github.smithjustinn.blackjack.model
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
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
