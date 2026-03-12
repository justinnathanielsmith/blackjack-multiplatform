package io.github.smithjustinn.blackjack

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BlackjackStateMachineTest {
    @Test
    fun testInitialDeal() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.NewGame)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHand.cards.size)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(48, state.deck.size)
        }

    @Test
    fun testPlayerHit() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.NewGame)
            advanceUntilIdle()

            // Skip test if initial deal resulted in blackjack (game already over)
            if (stateMachine.state.value.status != GameStatus.PLAYING) return@runTest

            val initialPlayerCards = stateMachine.state.value.playerHand.cards.size
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(initialPlayerCards + 1, stateMachine.state.value.playerHand.cards.size)
            assertEquals(47, stateMachine.state.value.deck.size)
        }

    @Test
    fun testBust() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.NewGame)
            advanceUntilIdle()

            // Hit until bust
            while (stateMachine.state.value.status == GameStatus.PLAYING) {
                stateMachine.dispatch(GameAction.Hit)
                advanceUntilIdle()
            }

            if (stateMachine.state.value.playerHand.isBust) {
                assertEquals(GameStatus.DEALER_WON, stateMachine.state.value.status)
            }
        }
}
