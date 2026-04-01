package io.github.smithjustinn.blackjack

import persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HandCountPersistenceTest {
    @Test
    fun testNewGameRemembersHandCount() =
        runTest {
            val stateMachine = testMachine()

            // Start with 3 hands
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 1000,
                    handCount = 3
                )
            )
            testScheduler.advanceUntilIdle()
            assertEquals(3, stateMachine.state.value.handCount)

            // Start another game with 2 hands
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 1000,
                    handCount = 2
                )
            )
            testScheduler.advanceUntilIdle()
            assertEquals(2, stateMachine.state.value.handCount)
        }

    @Test
    fun testSelectHandCountPersistsInCurrentState() =
        runTest {
            val stateMachine = testMachine()

            // Initial state is 1 hand
            stateMachine.dispatch(GameAction.NewGame(initialBalance = 1000, handCount = 1))
            testScheduler.advanceUntilIdle()
            assertEquals(1, stateMachine.state.value.handCount)

            // Change to 2 hands via SelectHandCount action
            stateMachine.dispatch(GameAction.SelectHandCount(2))
            testScheduler.advanceUntilIdle()
            assertEquals(2, stateMachine.state.value.handCount)
        }

    @Test
    fun testNewGameCorrectlyInitializesCurrentBets() =
        runTest {
            val stateMachine = testMachine()

            // Start with 3 hands and 100 bet on first hand
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 1000,
                    handCount = 3,
                    previousBets = persistentListOf(100)
                )
            )
            testScheduler.advanceUntilIdle()
            val state = stateMachine.state.value
            assertEquals(3, state.handCount)
            assertEquals(3, state.playerHands.size)
            assertEquals(100, state.playerHands[0].bet)
            assertEquals(0, state.playerHands[1].bet)
            assertEquals(0, state.playerHands[2].bet)
        }
}
