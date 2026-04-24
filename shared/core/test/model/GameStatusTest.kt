package io.github.smithjustinn.blackjack.model

import kotlin.test.Test
import kotlin.test.assertEquals

class GameStatusTest {
    @Test
    fun testIsTerminal() {
        val terminalStates =
            setOf(
                GameStatus.PLAYER_WON,
                GameStatus.DEALER_WON,
                GameStatus.PUSH
            )

        for (status in GameStatus.entries) {
            val expected = status in terminalStates
            assertEquals(
                expected,
                status.isTerminal(),
                "isTerminal() failed for status: $status"
            )
        }
    }

    @Test
    fun testIsProcess() {
        val processStates =
            setOf(
                GameStatus.DEALING,
                GameStatus.DEALER_TURN
            )

        for (status in GameStatus.entries) {
            val expected = status in processStates
            assertEquals(
                expected,
                status.isProcess(),
                "isProcess() failed for status: $status"
            )
        }
    }

    @Test
    fun testIsStatusVisible() {
        for (status in GameStatus.entries) {
            val expected = status.isProcess() || status.isTerminal()
            assertEquals(
                expected,
                status.isStatusVisible(),
                "isStatusVisible() failed for status: $status"
            )
        }
    }
}
