@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [GameEffect.DealerCriticalDraw] and [GameEffect.PlayPushSound] emission —
 * two effects with zero prior coverage in the effects-flow suite.
 *
 * DealerCriticalDraw fires exactly once per card drawn when the dealer's hand is in
 * the hard-stiff range [DEALER_STIFF_MIN(12) until DEALER_STAND_THRESHOLD(17)] and is
 * not soft. PlayPushSound fires in finalizeGame() when every player hand pushes.
 */
class DealerEffectsFlowTest {
    // ── DealerCriticalDraw ────────────────────────────────────────────────────

    @Test
    fun dealerCriticalDrawEmittedOnHardStiffHand() =
        runTest {
            // Dealer 7+5=12 (hard stiff minimum); player 19 stands; deck provides one draw
            // Expected: DealerCriticalDraw fired before the card is drawn, then player wins
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.SEVEN, Rank.FIVE),
                        deck = deckOf(Rank.SIX), // dealer 12+6=18 → stands; player 19 wins
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // DealerCriticalDraw, PlayCardSound, ChipEruption(200), PlayWinSound, WinPulse
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawEmittedAtBoundaryHardSixteen() =
        runTest {
            // Dealer 9+7=16 (hard stiff upper boundary); player 19 stands
            // Expected: DealerCriticalDraw fired — 16 is the highest stiff value
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.NINE, Rank.SEVEN),
                        deck = deckOf(Rank.TWO), // dealer 16+2=18 → stands; player 19 wins
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // DealerCriticalDraw, PlayCardSound, ChipEruption(200), PlayWinSound, WinPulse
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawNotEmittedForSoftHand() =
        runTest {
            // Dealer A+5=soft 16: must draw (score < 17) but isCritical=false because isSoft=true
            // Expected: PlayCardSound for the draw, but NO DealerCriticalDraw
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = dealerHand(Rank.ACE, Rank.FIVE),
                        deck = deckOf(Rank.FOUR), // dealer A+5+4=soft 20 → stands; dealer wins
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // PlayCardSound, ChipLoss(100), PlayLoseSound, Vibrate — no DealerCriticalDraw
                val emitted = buildList { repeat(4) { add(awaitItem()) } }
                assertFalse(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerCriticalDrawNotEmittedWhenDealerStandsAtSeventeen() =
        runTest {
            // Dealer already at hard 17 — shouldDealerDraw returns false immediately;
            // the stiff-range check is never reached
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.NINE),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // ChipEruption(200), PlayWinSound, WinPulse — no DealerCriticalDraw
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.DealerCriticalDraw in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── PlayPushSound ─────────────────────────────────────────────────────────

    @Test
    fun pushEmitsPlayPushSound() =
        runTest {
            // Player TEN+SEVEN=17 ties dealer TEN+SEVEN=17 — finalizeGame sets PUSH
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SEVEN),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // ChipEruption(100) (push returns bet), PlayPushSound
                val emitted = buildList { repeat(2) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerWinDoesNotEmitPlayPushSound() =
        runTest {
            // Player TEN+KING=20 beats dealer TEN+SEVEN=17 — outcome is PLAYER_WON, not PUSH
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.KING),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // ChipEruption(200), PlayWinSound, WinPulse — no PlayPushSound
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerWinDoesNotEmitPlayPushSound() =
        runTest {
            // Player SEVEN+SEVEN=14 loses to dealer TEN+SEVEN=17 — outcome is DEALER_WON
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.SEVEN, Rank.SEVEN),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // ChipLoss(100), PlayLoseSound, Vibrate — no PlayPushSound
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(GameEffect.PlayPushSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
