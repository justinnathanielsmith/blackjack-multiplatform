package io.github.smithjustinn.blackjack.state

import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.util.bettingState
import kotlinx.collections.immutable.persistentListOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LifecycleActionTest {
    @Test
    fun reduce_NewGame_returnsInitialState() {
        val initialState = bettingState(balance = 500)
        val action = GameAction.NewGame(initialBalance = 1000, handCount = 2)

        val result = reduce(initialState, action)

        assertEquals(1000, result.state.balance)
        assertEquals(2, result.state.handCount)
        assertEquals(GameStatus.BETTING, result.state.status)
        assertEquals(2, result.state.playerHands.size)
    }

    @Test
    fun reduce_Deal_transitionsToDealingWhenValid() {
        val state =
            bettingState(balance = 900, handCount = 1).copy(
                playerHands = persistentListOf(Hand(bet = 100))
            )
        val action = GameAction.Deal

        val result = reduce(state, action)

        assertEquals(GameStatus.DEALING, result.state.status)
        assertEquals(1, result.commands.size)
        assertEquals(ReducerCommand.RunDealSequence, result.commands[0])
    }

    @Test
    fun reduce_Deal_isRejectedWhenNoBet() {
        val state = bettingState(balance = 1000, handCount = 1) // bet is 0 by default
        val action = GameAction.Deal

        val result = reduce(state, action)

        assertEquals(GameStatus.BETTING, result.state.status)
        assertEquals(state, result.state)
        assertTrue(result.effects.contains(GameEffect.Vibrate))
    }
}
