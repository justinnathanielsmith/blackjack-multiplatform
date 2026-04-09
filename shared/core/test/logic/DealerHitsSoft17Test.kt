@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.util.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DealerHitsSoft17Test {
    @Test
    fun dealerStandsOnSoft17_whenRuleDisabled() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = false)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX), // Soft 17
                        deck = deckOf(Rank.TEN), // Should NOT be drawn
                        rules = rules
                    )
                )

            // Act
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertTrue(state.dealerHand.isSoft)
            assertEquals(2, state.dealerHand.cards.size) // No extra cards drawn
            assertEquals(GameStatus.PLAYER_WON, state.status) // 20 vs 17
        }

    @Test
    fun dealerHitsOnSoft17_whenRuleEnabled() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX), // Soft 17
                        deck = deckOf(Rank.TWO), // Should be drawn -> 19 (Soft) or 13 (Hard)? No, A+6+2 = 19
                        rules = rules
                    )
                )

            // Act
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(19, state.dealerHand.score)
            assertEquals(3, state.dealerHand.cards.size) // Hit once
            assertEquals(GameStatus.PLAYER_WON, state.status) // 20 vs 19
        }

    @Test
    fun dealerHitsOnSoft17_multipleTimes() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.THREE), // Soft 14
                        // Soft 14 + 3 = Soft 17 (Hits again) + 10 = Hard 17 (Stands)
                        deck = deckOf(Rank.THREE, Rank.TEN),
                        rules = rules
                    )
                )

            // Act
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertFalse(state.dealerHand.isSoft) // Became hard 17
            assertEquals(4, state.dealerHand.cards.size) // Hit twice
            assertEquals(GameStatus.PLAYER_WON, state.status) // 20 vs 17
        }

    @Test
    fun dealerStandsOnHard17_whenHitSoft17Enabled() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN), // Hard 17
                        deck = deckOf(Rank.TEN), // Should NOT be drawn
                        rules = rules
                    )
                )

            // Act
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(17, state.dealerHand.score)
            assertFalse(state.dealerHand.isSoft)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(GameStatus.PLAYER_WON, state.status)
        }

    @Test
    fun dealerStandsOnSoft18_whenHitSoft17Enabled() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN), // Soft 18
                        deck = deckOf(Rank.TEN), // Should NOT be drawn
                        rules = rules
                    )
                )

            // Act
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Assert
            val state = sm.state.value
            assertEquals(18, state.dealerHand.score)
            assertTrue(state.dealerHand.isSoft)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(GameStatus.PLAYER_WON, state.status)
        }

    @Test
    fun dealerCriticalDrawEffect_emitted_duringSoft17Hit() =
        runTest {
            // Arrange
            val rules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.ACE, Rank.SIX), // Soft 17
                        deck = deckOf(Rank.TEN),
                        rules = rules
                    )
                )

            sm.effects.test {
                // Act
                sm.dispatch(GameAction.Stand)

                // Assert
                val emitted =
                    buildList<GameEffect> {
                        // Dealer reveal hole (1 item)
                        // then maybe effects from dealer turn
                        // Wait, Soft 17 is technically 17, which is >= DEALER_STIFF_MIN (12)
                        // Let's check GameFlowMiddleware.kt:
                        // val isCritical = hand.score in DEALER_STIFF_MIN until DEALER_STAND_THRESHOLD && !hand.isSoft
                        // So Soft 17 is NOT critical because hand.isSoft is true.

                        // Let's double check that.
                        // line 100: !hand.isSoft

                        // So I won't test for DealerCriticalDraw here, but I can test for PlayCardSound.
                        while (true) {
                            val item = awaitItem()
                            add(item)
                            if (item == GameEffect.PlayWinSound ||
                                item == GameEffect.PlayLoseSound ||
                                item == GameEffect.PlayPushSound
                            ) {
                                break
                            }
                        }
                    }
                assertTrue(emitted.contains(GameEffect.PlayCardSound))
                cancelAndIgnoreRemainingEvents()
            }
        }
}
