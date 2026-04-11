@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.middleware
import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.playingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [GameEffect.DealerCriticalDraw] and [GameEffect.PlayPushSound] emission —
 * two effects with zero prior coverage in the effects-flow suite.
 */
class DealerEffectsFlowTest {
    // ── DealerCriticalDraw ────────────────────────────────────────────────────

    @Test
    fun dealerCriticalDrawEmittedOnHardStiffHand() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.SEVEN, Rank.FIVE),
                        deck = deckOf(Rank.SIX),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawEmittedAtBoundaryHardSixteen() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.NINE, Rank.SEVEN),
                        deck = deckOf(Rank.TWO),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawNotEmittedForSoftHand() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.ACE, Rank.FIVE),
                        deck = deckOf(Rank.FOUR),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(4) { add(awaitItem()) } }
                assertFalse(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawNotEmittedWhenDealerStandsAtSeventeen() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── PlayPushSound ─────────────────────────────────────────────────────────

    @Test
    fun pushEmitsPlayPushSound() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SEVEN),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(2) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerWinDoesNotEmitPlayPushSound() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.KING),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerWinDoesNotEmitPlayPushSound() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.SEVEN, Rank.SEVEN),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
