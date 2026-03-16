package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerActionLogicTest {

    @Test
    fun stand_returnsNoop_whenGameNotPlaying() {
        val state = GameState(status = GameStatus.PLAYER_WON)
        val outcome = PlayerActionLogic.stand(state)

        assertEquals(state, outcome.state)
        assertFalse(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }

    @Test
    fun stand_returnsTrueForAdvanceTurn_whenGameIsPlaying() {
        val state = playingState(
            playerHand = hand(Rank.TEN, Rank.TEN),
            dealerHand = dealerHand(Rank.TEN, Rank.NINE)
        )
        val outcome = PlayerActionLogic.stand(state)

        assertEquals(state, outcome.state)
        assertTrue(outcome.shouldAdvanceTurn)
        assertTrue(outcome.effects.isEmpty())
    }
}
