@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.middleware

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.util.bettingState
import io.github.smithjustinn.blackjack.util.card
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.deckOf
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.playingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEffectsFlowTest {
    @Test
    fun hitEmitsPlayCardSound() =
        runTest {
            // Player EIGHT+THREE=11, draws FIVE → 16 (no bust, game continues)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FIVE),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                assertTrue(GameEffect.PlayCardSound in buildList { repeat(2) { add(awaitItem()) } })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerWinEmitsPlayWinSound() =
        runTest {
            // Player TEN+KING=20 stands, dealer TEN+SEVEN=17 (player wins)
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
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayWinSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerWinEmitsWinPulse() =
        runTest {
            // Player TEN+KING=20 stands, dealer TEN+SEVEN=17 (player wins)
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
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.WinPulse in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerBust_emitsCompleteEffectSequence() =
        runTest {
            // Player TEN+TEN=20, draws TEN → 30 (guaranteed bust)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.TEN),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                // Emissions: PlayCardSound, HeavyCardThud (TEN drawn), BustThud, ChipLoss, PlayLoseSound
                val emitted = buildList { repeat(5) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayCardSound in emitted)
                assertTrue(GameEffect.HeavyCardThud in emitted)
                assertTrue(GameEffect.BustThud in emitted)
                assertTrue(emitted.any { it is GameEffect.ChipLoss })
                assertTrue(GameEffect.PlayLoseSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitFaceCardEmitsHeavyCardThud() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.HeavyCardThud in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitToExactly21EmitsPulse21() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.Pulse21 in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun dealerWinEmitsPlayLoseSoundAndVibrate() =
        runTest {
            // Player TEN+SIX=16 stands, dealer TEN+NINE=19 wins
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = hand(Rank.TEN, Rank.NINE),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Stand)
                // ChipLoss, PlayLoseSound, Vibrate
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.PlayLoseSound in emitted)
                assertTrue(GameEffect.Vibrate in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitNonFaceCardEmitsLightTick() =
        runTest {
            // Player EIGHT+THREE=11, draws FOUR → 15 (non-face card)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.FOUR),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(2) { add(awaitItem()) } }
                assertTrue(GameEffect.LightTick in emitted)
                assertFalse(GameEffect.HeavyCardThud in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitFaceCardEmitsHeavyCardThudNotLightTick() =
        runTest {
            // Player EIGHT+THREE=11, draws KING → 21 (face card)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.EIGHT, Rank.THREE),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.KING),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(GameEffect.HeavyCardThud in emitted)
                assertFalse(GameEffect.LightTick in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun hitToScoreElevenEmitsNearMissHighlight() =
        runTest {
            // Player SIX+TWO=8, draws THREE → 11
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.SIX, Rank.TWO),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                        deck = deckOf(Rank.THREE),
                    ),
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Hit)
                // Emissions: PlayCardSound, LightTick, NearMissHighlight(0)
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertTrue(emitted.any { it is GameEffect.NearMissHighlight && it.handIndex == 0 })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun massiveSideBetWinEmitsBigWin() =
        runTest {
            // Perfect Pair (same rank, same suit) pays 25:1
            val sm =
                testMachine(
                    bettingState(balance = 1000).copy(
                        playerHands = persistentListOf(Hand(bet = 100)),
                        sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                        deck =
                            deckOf(
                                Rank.ACE,
                                Rank.TEN, // Round 1: Player ACE, Dealer TEN
                                Rank.ACE,
                                Rank.TEN, // Round 2: Player ACE, Dealer TEN (face-down)
                            ),
                    )
                )

            sm.effects.test {
                runCurrent()
                sm.dispatch(GameAction.Deal)
                advanceUntilIdle()

                // Deal sequence emits 4 cards: PlayCardSound × 4
                repeat(4) { assertTrue(awaitItem() is GameEffect.PlayCardSound) }

                val emitted =
                    buildList {
                        add(awaitItem()) // ChipEruption for SideBet
                        add(awaitItem()) // BigWin
                    }
                assertTrue(emitted.any { it is GameEffect.BigWin && it.totalPayout == 2600 })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun subThresholdSideBetWinEmitsPlayWinSoundNotBigWin() =
        runTest {
            // COLORED_PAIR: same rank, different color → 12× < 25 → sub-threshold
            val deck =
                persistentListOf(
                    card(Rank.FIVE, Suit.HEARTS),
                    card(Rank.TEN, Suit.CLUBS),
                    card(Rank.FIVE, Suit.SPADES),
                    card(Rank.TEN, Suit.DIAMONDS, faceDown = true),
                )
            val sm =
                testMachine(
                    bettingState(balance = 900).copy(
                        playerHands = persistentListOf(Hand(bet = 100)),
                        sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                        deck = deck,
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Deal)
                advanceUntilIdle()

                // 4 deal sounds
                repeat(4) { assertTrue(awaitItem() is GameEffect.PlayCardSound) }

                // ChipEruption for the COLORED_PAIR win, then PlayWinSound
                val afterDeal = buildList { repeat(2) { add(awaitItem()) } }
                assertTrue(afterDeal.any { it is GameEffect.ChipEruption })
                assertTrue(
                    GameEffect.PlayWinSound in afterDeal,
                    "Expected PlayWinSound for sub-threshold side bet (12× < 25), got: $afterDeal",
                )
                assertFalse(
                    afterDeal.any { it is GameEffect.BigWin },
                    "BigWin must NOT fire for COLORED_PAIR (multiplier 12 < MASSIVE_WIN_MULTIPLIER 25)",
                )

                cancelAndIgnoreRemainingEvents()
            }
        }
}
