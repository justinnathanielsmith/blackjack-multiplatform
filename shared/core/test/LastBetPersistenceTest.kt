package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class LastBetPersistenceTest {
    @Test
    fun testMultiHandBetPersistence() = runTest {
        val stateMachine = testMachine()

        // 1. Initial State: 3 seats, $0 bets
        stateMachine.dispatch(GameAction.NewGame(initialBalance = 1000, handCount = 3))
        testScheduler.advanceUntilIdle()
        assertEquals(3, stateMachine.state.value.handCount)
        assertEquals(persistentListOf(0, 0, 0), stateMachine.state.value.currentBets)

        // 2. Place different bets on each seat
        stateMachine.dispatch(GameAction.PlaceBet(amount = 10, seatIndex = 0))
        stateMachine.dispatch(GameAction.PlaceBet(amount = 25, seatIndex = 1))
        stateMachine.dispatch(GameAction.PlaceBet(amount = 50, seatIndex = 2))
        testScheduler.advanceUntilIdle()
        
        val lastBets = stateMachine.state.value.currentBets
        assertEquals(persistentListOf(10, 25, 50), lastBets)
        assertEquals(1000 - 10 - 25 - 50, stateMachine.state.value.balance)

        // 3. Round ends - start new game passing the last bets
        stateMachine.dispatch(
            GameAction.NewGame(
                handCount = 3,
                lastBets = lastBets
            )
        )
        testScheduler.advanceUntilIdle()

        // 4. Verify new game initialized with those bets
        val newState = stateMachine.state.value
        assertEquals(3, newState.handCount)
        assertEquals(persistentListOf(10, 25, 50), newState.currentBets)
        assertEquals(lastBets, newState.lastBets)
        
        // Balance should be (1000 - 85) [previous end] - 85 [new bets placed] = 830
        // Wait, handleNewGame uses initialBalance if provided, else current balance.
        // Here we didn't provide initialBalance, so it uses 915.
        // 915 - 85 = 830.
        assertEquals(830, newState.balance)
    }

    @Test
    fun testMultiHandBetPersistence_InsufficientBalance() = runTest {
        val stateMachine = testMachine()

        // 1. Start with high bets
        stateMachine.dispatch(GameAction.NewGame(initialBalance = 200, handCount = 2))
        stateMachine.dispatch(GameAction.PlaceBet(amount = 100, seatIndex = 0))
        stateMachine.dispatch(GameAction.PlaceBet(amount = 100, seatIndex = 1))
        testScheduler.advanceUntilIdle()
        
        val lastBets = stateMachine.state.value.currentBets
        assertEquals(persistentListOf(100, 100), lastBets)
        assertEquals(0, stateMachine.state.value.balance)

        // 2. Lose the round (balance stays 0 implicitly in this test if we just start new game)
        // Actually, let's just start a new game with lower balance to simulate a loss.
        stateMachine.dispatch(
            GameAction.NewGame(
                initialBalance = 150, // Not enough for 100 + 100
                handCount = 2,
                lastBets = lastBets
            )
        )
        testScheduler.advanceUntilIdle()

        // 3. Verify bets reset to 0 because of insufficient balance
        val newState = stateMachine.state.value
        assertEquals(persistentListOf(0, 0), newState.currentBets)
        assertEquals(150, newState.balance)
        // But lastBets should still store the attempt
        assertEquals(lastBets, newState.lastBets)
    }
}
