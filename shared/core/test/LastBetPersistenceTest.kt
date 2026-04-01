package io.github.smithjustinn.blackjack

import persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LastBetPersistenceTest {
    @Test
    fun testMultiHandBetPersistence() =
        runTest {
            val stateMachine = testMachine()

            // 1. Initial State: 3 seats, $0 bets
            stateMachine.dispatch(GameAction.NewGame(initialBalance = 1000, handCount = 3))
            testScheduler.advanceUntilIdle()
            assertEquals(3, stateMachine.state.value.handCount)
            assertEquals(
                listOf(0, 0, 0),
                stateMachine.state.value.playerHands
                    .map { it.bet }
            )

            // 2. Place different bets on each seat
            stateMachine.dispatch(GameAction.PlaceBet(amount = 10, seatIndex = 0))
            stateMachine.dispatch(GameAction.PlaceBet(amount = 25, seatIndex = 1))
            stateMachine.dispatch(GameAction.PlaceBet(amount = 50, seatIndex = 2))
            testScheduler.advanceUntilIdle()

            val previousBets =
                stateMachine.state.value.playerHands
                    .map { it.bet }
                    .toPersistentList()
            assertEquals(persistentListOf(10, 25, 50), previousBets)
            assertEquals(1000 - 10 - 25 - 50, stateMachine.state.value.balance)

            // 3. Round ends - start new game passing the previous bets
            stateMachine.dispatch(
                GameAction.NewGame(
                    handCount = 3,
                    previousBets = previousBets
                )
            )
            testScheduler.advanceUntilIdle()

            // 4. Verify new game initialized with those bets
            val newState = stateMachine.state.value
            assertEquals(3, newState.handCount)
            assertEquals(listOf(10, 25, 50), newState.playerHands.map { it.bet })
            assertEquals(previousBets, newState.playerHands.map { it.lastBet }.toPersistentList())

            // Balance should be (1000 - 85) [previous end] - 85 [new bets placed] = 830
            assertEquals(830, newState.balance)
        }

    @Test
    fun testMultiHandBetPersistence_InsufficientBalance() =
        runTest {
            val stateMachine = testMachine()

            // 1. Start with high bets
            stateMachine.dispatch(GameAction.NewGame(initialBalance = 200, handCount = 2))
            stateMachine.dispatch(GameAction.PlaceBet(amount = 100, seatIndex = 0))
            stateMachine.dispatch(GameAction.PlaceBet(amount = 100, seatIndex = 1))
            testScheduler.advanceUntilIdle()

            val previousBets =
                stateMachine.state.value.playerHands
                    .map { it.bet }
                    .toPersistentList()
            assertEquals(persistentListOf(100, 100), previousBets)
            assertEquals(0, stateMachine.state.value.balance)

            // 2. Lose the round (balance stays 0 implicitly in this test if we just start new game)
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = 150, // Not enough for 100 + 100
                    handCount = 2,
                    previousBets = previousBets
                )
            )
            testScheduler.advanceUntilIdle()

            // 3. Verify bets reset to 0 because of insufficient balance
            val newState = stateMachine.state.value
            assertEquals(listOf(0, 0), newState.playerHands.map { it.bet })
            assertEquals(150, newState.balance)
            // But lastBets should still store the attempt
            assertEquals(previousBets, newState.playerHands.map { it.lastBet }.toPersistentList())
        }
}
