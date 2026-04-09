package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
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

    @Test
    fun testLastBetsCapturedAtDeal_IgnoresDoubleDown() =
        runTest {
            val sm = testMachine()
            // Deterministic deck: Player 5,10. Dealer 10,7. (Total 15 player, total 17 dealer)
            // Sequence: P1=5, D1=10, P2=6, D2=7
            val deck = deckOf(Rank.FIVE, Rank.TEN, Rank.SIX, Rank.SEVEN, Rank.TWO)

            // 1. Start game, place 10
            sm.dispatch(GameAction.NewGame(initialBalance = 1000, handCount = 1))
            sm.dispatch(GameAction.PlaceBet(amount = 10))
            sm.dispatch(GameAction.SetDeck(deck)) // Use fixed deck
            testScheduler.advanceUntilIdle()

            // 2. Deal — this should capture the 10
            sm.dispatch(GameAction.Deal)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(10), sm.state.value.lastBets)
            assertEquals(GameStatus.PLAYING, sm.state.value.status)

            // 3. Double Down — this increases bet to 20
            sm.dispatch(GameAction.DoubleDown)
            testScheduler.advanceUntilIdle()

            // 4. Current bet is 20, but lastBets MUST remain 10
            assertEquals(
                20,
                sm.state.value.playerHands[0]
                    .bet
            )
            assertEquals(listOf(10), sm.state.value.lastBets)
        }
}
