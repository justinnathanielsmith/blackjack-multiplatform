package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SideBetPersistenceTest {
    @Test
    fun testNewGameRemembersSideBets() =
        runTest {
            val lastSideBets =
                persistentMapOf(
                    SideBetType.PERFECT_PAIRS to 10,
                    SideBetType.TWENTY_ONE_PLUS_THREE to 25
                )

            val stateMachine =
                testMachine()
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 1000,
                    lastSideBets = lastSideBets
                )
            )
            testScheduler.advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(10, state.sideBets[SideBetType.PERFECT_PAIRS])
            assertEquals(25, state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE])
            // 1000 - 10 - 25 = 965
            assertEquals(965, state.balance)
            assertEquals(lastSideBets, state.lastSideBets)
        }

    @Test
    fun testNewGameRemembersSideBets_InsufficientBalance() =
        runTest {
            val lastSideBets =
                persistentMapOf(
                    SideBetType.PERFECT_PAIRS to 100,
                    SideBetType.TWENTY_ONE_PLUS_THREE to 100
                )

            val stateMachine =
                testMachine()
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 150,
                    lastBets = kotlinx.collections.immutable.persistentListOf(50),
                    lastSideBets = lastSideBets
                )
            )
            testScheduler.advanceUntilIdle()

            val state = stateMachine.state.value
            // Total cost: 50 (main) + 100 + 100 = 250.
            // 150 < 250, so side bets should NOT be placed.
            assertEquals(0, state.sideBets.size)
            assertEquals(50, state.currentBet)
            assertEquals(100, state.balance)
            assertEquals(lastSideBets, state.lastSideBets)
        }
}
