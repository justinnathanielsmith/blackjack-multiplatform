@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Verifies the [GameAction.UpdateRules] action in the [BlackjackStateMachine].
 * Rules should only be updatable during the [GameStatus.BETTING] phase.
 */
class UpdateRulesTest {
    @Test
    fun updateRules_duringBetting_updatesState() =
        runTest {
            val sm = testMachine(GameState(status = GameStatus.BETTING))
            val initialRules = sm.state.value.rules
            val newRules = initialRules.copy(dealerHitsSoft17 = false, allowSurrender = true)

            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            assertEquals(newRules, sm.state.value.rules, "Rules should be updated during BETTING status")
        }

    @Test
    fun updateRules_duringPlaying_isIgnored() =
        runTest {
            val initialRules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    playingState(
                        rules = initialRules,
                        playerHand = hand(Rank.TEN, Rank.TEN),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN)
                    )
                )
            val newRules = initialRules.copy(dealerHitsSoft17 = false)

            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            assertEquals(initialRules, sm.state.value.rules, "Rules should NOT be updated during PLAYING status")
            assertNotEquals(newRules, sm.state.value.rules)
        }

    @Test
    fun updateRules_duringDealerTurn_isIgnored() =
        runTest {
            val initialRules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.DEALER_TURN,
                        rules = initialRules,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.TEN)),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN)
                    )
                )
            val newRules = initialRules.copy(dealerHitsSoft17 = false)

            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            assertEquals(initialRules, sm.state.value.rules, "Rules should NOT be updated during DEALER_TURN status")
        }

    @Test
    fun updateRules_duringTerminalStatus_isIgnored() =
        runTest {
            val initialRules = GameRules(dealerHitsSoft17 = true)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYER_WON,
                        rules = initialRules,
                        playerHands = persistentListOf(hand(Rank.ACE, Rank.TEN)),
                        dealerHand = hand(Rank.TEN, Rank.SEVEN)
                    )
                )
            val newRules = initialRules.copy(dealerHitsSoft17 = false)

            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            assertEquals(initialRules, sm.state.value.rules, "Rules should NOT be updated during terminal status")
        }

    @Test
    fun updateRules_integration_affectsGameplay() =
        runTest {
            // Setup: Disable S17 (Dealer stands on Soft 17)
            // Construct deck to force Dealer Soft 17:
            // P1: TEN, D1: SIX (upcard), P2: TEN (P=20), D2: ACE (D=Soft 17)
            val initialDeck =
                deckOf(
                    Rank.TEN,
                    Rank.SIX,
                    Rank.TEN,
                    Rank.ACE,
                )

            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        playerHands = persistentListOf(Hand(bet = 100)),
                        deck = initialDeck,
                    ),
                )

            val newRules = GameRules(dealerHitsSoft17 = false)
            sm.dispatch(GameAction.UpdateRules(newRules))
            advanceUntilIdle()

            // Deal
            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Player stands at 20
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            // Outcome: With S17 disabled, dealer should STAND at 17.
            // If dealer stands at 17 and player has 20, player wins.
            assertEquals(GameStatus.PLAYER_WON, sm.state.value.status)
            assertEquals(17, sm.state.value.dealerHand.score)
            assertEquals(2, sm.state.value.dealerHand.cards.size, "Dealer should have only 2 cards (stood at 17)")
        }
}
