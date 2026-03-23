@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SideBetActionTest {
    @Test
    fun testPlaceSideBet_updatesState() =
        runTest {
            val sm = testMachine(
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                )
            )

            sm.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 50))
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(50, state.sideBets[SideBetType.PERFECT_PAIRS])
            assertEquals(950, state.balance)
        }

    @Test
    fun testResetSideBets_updatesState() =
        runTest {
            val sm = testMachine(
                GameState(
                    status = GameStatus.BETTING,
                    balance = 950,
                    sideBets = kotlinx.collections.immutable.persistentMapOf(SideBetType.PERFECT_PAIRS to 50)
                )
            )

            sm.dispatch(GameAction.ResetSideBets)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(0, state.sideBets.size)
            assertEquals(1000, state.balance)
        }

    @Test
    fun testSideBets_clearedAfterDeal() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    playerHands = kotlinx.collections.immutable.persistentListOf(Hand(bet = 100)),
                    sideBets = kotlinx.collections.immutable.persistentMapOf(SideBetType.PERFECT_PAIRS to 50),
                    deck =
                        kotlinx.collections.immutable.persistentListOf(
                            Card(Rank.TEN, Suit.SPADES), // P1
                            Card(Rank.SEVEN, Suit.HEARTS), // D1
                            Card(Rank.TEN, Suit.SPADES), // P2
                            Card(Rank.EIGHT, Suit.DIAMONDS), // D2
                        ),
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            // Verification: sideBets should be cleared and moved to lastSideBets
            assertEquals(0, state.sideBets.size, "sideBets should be cleared after deal")
            assertEquals(50, state.lastSideBets[SideBetType.PERFECT_PAIRS], "lastSideBets should be updated")
            assertEquals(1, state.sideBetResults.size, "should have 1 result (win)")
        }
}
