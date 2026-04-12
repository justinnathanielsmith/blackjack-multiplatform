@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.bettingState
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.playingState
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Verifies that all illegal actions in the engine correctly emit a [GameEffect.Vibrate]
 * to provide consistent haptic feedback to the player.
 */
class ActionFailureFeedbackTest {
    @Test
    fun testHit_on21_vibrates() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.TEN, Rank.TEN, Rank.ACE), // 21
                        dealerHand = hand(Rank.FIVE)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                assertEquals(GameEffect.Vibrate, awaitItem(), "Higher score hit should vibrate")
            }
        }

    @Test
    fun testHit_onSplitAces_vibrates() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        playerHand = hand(Rank.ACE, Rank.TEN).copy(isFromSplitAce = true),
                        dealerHand = hand(Rank.FIVE)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Hit)
                assertEquals(GameEffect.Vibrate, awaitItem(), "Split Ace hit should vibrate")
            }
        }

    @Test
    fun testDoubleDown_insufficientBalance_vibrates() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        balance = 50,
                        bet = 100,
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = hand(Rank.FIVE)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.DoubleDown)
                assertEquals(GameEffect.Vibrate, awaitItem(), "Low balance double should vibrate")
            }
        }

    @Test
    fun testSurrender_rulesDisabled_vibrates() =
        runTest {
            val sm =
                testMachine(
                    playingState(
                        rules = GameRules(allowSurrender = false),
                        playerHand = hand(Rank.TEN, Rank.SIX),
                        dealerHand = hand(Rank.TEN)
                    )
                )

            sm.effects.test {
                sm.dispatch(GameAction.Surrender)
                assertEquals(GameEffect.Vibrate, awaitItem(), "Disabled surrender should vibrate")
            }
        }

    @Test
    fun testPlaceSideBet_insufficientBalance_vibrates() =
        runTest {
            val sm = testMachine(bettingState(balance = 40))

            sm.effects.test {
                sm.dispatch(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, 50))
                assertEquals(GameEffect.Vibrate, awaitItem(), "Low balance side bet should vibrate")
            }
        }

    @Test
    fun testResetSideBet_missingBet_vibrates() =
        runTest {
            val sm = testMachine(bettingState())

            sm.effects.test {
                sm.dispatch(GameAction.ResetSideBet(SideBetType.PERFECT_PAIRS))
                assertEquals(GameEffect.Vibrate, awaitItem(), "Missing side bet reset should vibrate")
            }
        }

    @Test
    fun testActionsInWrongStatus_vibrate() =
        runTest {
            val playing = playingState(playerHand = hand(Rank.TEN, Rank.SIX), dealerHand = hand(Rank.TEN))
            val betting = bettingState()

            val scenarios =
                listOf(
                    playing to GameAction.PlaceBet(10, 0),
                    playing to GameAction.SelectHandCount(2),
                    playing to GameAction.ResetBet,
                    betting to GameAction.Hit,
                    betting to GameAction.Stand,
                    betting to GameAction.DoubleDown,
                    betting to GameAction.Split,
                    betting to GameAction.TakeInsurance
                )

            scenarios.forEach { (state, action) ->
                val sm = testMachine(state)
                sm.effects.test {
                    sm.dispatch(action)
                    assertEquals(
                        GameEffect.Vibrate,
                        awaitItem(),
                        "Action $action in ${state.status} mode should vibrate"
                    )
                }
            }
        }
}
