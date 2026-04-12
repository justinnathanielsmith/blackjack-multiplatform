@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.util

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.state.BlackjackStateMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * Dispatches [action] and asserts exactly one new [GameState] is emitted that
 * satisfies [assertionBlock]. Handles the awaitItem / dispatch / awaitItem /
 * cancelAndIgnoreRemainingEvents boilerplate internally.
 *
 * Usage:
 * ```
 * sm.assertTransition(GameAction.PlaceBet(100)) { state ->
 *     assertEquals(900, state.balance)
 * }
 * ```
 */
suspend fun BlackjackStateMachine.assertTransition(
    action: GameAction,
    assertionBlock: (GameState) -> Unit,
) {
    state.test {
        awaitItem() // consume initial state
        dispatch(action)
        val newState = awaitItem()
        assertionBlock(newState)
        cancelAndIgnoreRemainingEvents()
    }
}

/**
 * Asserts that dispatching [action] produces **no** state change — i.e., the
 * action is silently rejected by the reducer.
 *
 * Usage:
 * ```
 * sm.assertNoTransition(GameAction.Hit) // guard: Hit is invalid outside PLAYING
 * ```
 */
suspend fun BlackjackStateMachine.assertNoTransition(action: GameAction) {
    state.test {
        awaitItem()
        dispatch(action)
        expectNoEvents()
        cancelAndIgnoreRemainingEvents()
    }
}
