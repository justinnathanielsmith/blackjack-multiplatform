@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.NewGameLogic
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.bettingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Verifies that the state machine correctly enforces betting rules and seat limits.
 *
 * This suite focuses on rejection paths: ensuring that invalid wagers or seat counts
 * are blocked and that the player receives immediate feedback via [GameEffect.Vibrate].
 */
class BettingEnforcementTest {
    @Test
    fun placeBet_negativeAmount_vibratesAndIsIgnored() =
        runTest {
            val sm = testMachine(bettingState(balance = 1000))

            sm.effects.test {
                sm.dispatch(GameAction.PlaceBet(amount = -50, seatIndex = 0))

                // Rejection should trigger a vibration effect
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            // State should remain unchanged
            assertEquals(1000, sm.state.value.balance)
            assertEquals(
                0,
                sm.state.value.playerHands[0]
                    .bet
            )
        }

    @Test
    fun placeBet_invalidSeatIndex_vibratesAndIsIgnored() =
        runTest {
            // Only 1 seat enabled
            val sm = testMachine(bettingState(balance = 1000, handCount = 1))

            sm.effects.test {
                // Attempt to bet on seat 2 (invalid)
                sm.dispatch(GameAction.PlaceBet(amount = 50, seatIndex = 2))

                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1000, sm.state.value.balance)
        }

    @Test
    fun placeBet_insufficientBalance_vibratesAndIsIgnored() =
        runTest {
            val sm = testMachine(bettingState(balance = 40))

            sm.effects.test {
                // Attempt to bet 50 with only 40 balance
                sm.dispatch(GameAction.PlaceBet(amount = 50, seatIndex = 0))

                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(40, sm.state.value.balance)
        }

    @Test
    fun selectHandCount_belowMinimum_vibratesAndIsIgnored() =
        runTest {
            val sm = testMachine(bettingState(handCount = 1))

            sm.effects.test {
                // BlackjackConfig.MIN_INITIAL_HANDS is 1
                sm.dispatch(GameAction.SelectHandCount(0))

                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, sm.state.value.handCount)
        }

    @Test
    fun selectHandCount_aboveMaximum_vibratesAndIsIgnored() =
        runTest {
            val sm = testMachine(bettingState(handCount = 1))

            sm.effects.test {
                // BlackjackConfig.MAX_INITIAL_HANDS is 3
                sm.dispatch(GameAction.SelectHandCount(4))

                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, sm.state.value.handCount)
        }

    @Test
    fun placeSideBet_insufficientBalance_isSilentlyIgnored() =
        runTest {
            // Note: Side bets currently do not emit Vibrate on failure (documented behavior)
            val sm = testMachine(bettingState(balance = 40))

            sm.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 50))

            // Wait for potential effects (none expected)
            testScheduler.advanceUntilIdle()

            assertEquals(40, sm.state.value.balance)
            assertEquals(0, sm.state.value.sideBets.size)
        }

    @Test
    fun newGame_invalidHandCount_throwsIllegalArgumentException() {
        // NewGameLogic.createInitialState uses require() to enforce bounds
        assertFailsWith<IllegalArgumentException> {
            NewGameLogic.createInitialState(balance = 1000, handCount = 0)
        }

        assertFailsWith<IllegalArgumentException> {
            NewGameLogic.createInitialState(balance = 1000, handCount = 4)
        }
    }
}
