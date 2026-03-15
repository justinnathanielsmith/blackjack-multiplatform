@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEffectsFlowTest {
    @Test
    fun hitEmitsPlayCardSound() =
        runTest {
            // Player EIGHT+THREE=11, draws FIVE → 16 (no bust, game continues)
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FIVE),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                assertEquals(GameEffect.PlayCardSound, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerWinEmitsPlayWinSound() =
        runTest {
            // Player TEN+KING=20 stands, dealer TEN+SEVEN=17 stands (no dealer draw)
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.KING),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                // ChipEruption is emitted before PlayWinSound
                val emitted = buildList { repeat(2) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayWinSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerBustEmitsPlayCardSoundThenLoseSoundAndVibrate() =
        runTest {
            // Player TEN+TEN=20, draws TEN → 30 (guaranteed bust)
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                // Emissions: PlayCardSound, HeavyCardThud (TEN drawn), ChipLoss, PlayLoseSound, Vibrate
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayCardSound in emitted)
                assertTrue(GameEffect.HeavyCardThud in emitted)
                assertTrue(GameEffect.PlayLoseSound in emitted)
                assertTrue(GameEffect.Vibrate in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitFaceCardEmitsHeavyCardThud() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.HeavyCardThud in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitLowCardDoesNotEmitHeavyCardThud() =
        runTest {
            // Player EIGHT+THREE=11, draws FOUR → 15
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FOUR),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(1) { add(awaitItem()) } }
                assertTrue(GameEffect.HeavyCardThud !in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitToExactly21EmitsPulse21() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.Pulse21 in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitKingToReach21EmitsBothHeavyCardThudAndPulse21() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.HeavyCardThud in emitted)
                assertTrue(GameEffect.Pulse21 in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerBustEmitsChipAnimationAndVibrateAndLoseSound() =
        runTest {
            // Player TEN+TEN=20, draws TEN → 30 (guaranteed bust)
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TEN),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                // Emissions include ChipLoss, PlayLoseSound, Vibrate on bust
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(emitted.any { it is GameEffect.ChipLoss })
                assertTrue(GameEffect.PlayLoseSound in emitted)
                assertTrue(GameEffect.Vibrate in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerWinEmitsPlayLoseSoundAndVibrate() =
        runTest {
            // Player TEN+SIX=16 stands, dealer TEN+NINE=19 wins
            val sm =
                BlackjackStateMachine(
                    this,
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                // ChipLoss is also emitted: ChipLoss, PlayLoseSound, Vibrate
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayLoseSound in emitted)
                assertTrue(GameEffect.Vibrate in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
