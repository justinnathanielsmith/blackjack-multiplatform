@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SideBetIntegrationTest {
    @Test
    fun sideBet_PerfectPair_Win_MainHand_Loss() =
        runTest {
            // Correct deck order: P, D, P, D
            // Indices: 0:P, 1:D, 2:P, 3:D
            // Player: ACE, ACE (Perfect Pair, score 12)
            // Dealer: TEN, TEN (score 20)
            val deck =
                deckOf(
                    Rank.ACE, // P1 (Index 0)
                    Rank.TEN, // D1 (Index 1 - Upcard)
                    Rank.ACE, // P2 (Index 2)
                    Rank.TEN // D2 (Index 3 - Hole)
                )
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 800,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                    deck = deck,
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Status should be PLAYING (Player has 12, Dealer has 20 but 1 is down)
            assertEquals(GameStatus.PLAYING, sm.state.value.status)

            // Player stands
            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            // Initial 800. Side bet win: 100 * 25 + 100 = 2600. Main hand loss: -100.
            // Wait, balance is updated at Deal for side bets.
            // Initial 800 -> Deal (Side bet win +2600, Balance 3400?)
            // No, the side bet bet (100) was ALREADY subtracted in GameState init or Deal?
            // "sideBets = ... 100" in GameState init doesn't subtract from balance automatically.
            // But Dispatch(Deal) calls applyInitialOutcome which does:
            // balance = current.balance + balanceUpdate + sideBetUpdate.payoutTotal
            // 800 + 0 + 2600 = 3400.
            // Then Stand -> resolveHand -> dealer wins.
            // Main bet was 100. resolveHand returns 0.
            // Payout total from resolveHand is added to balance?
            // Actually, for main hand, the balance is updated at the END of the round.
            // But the bet was NOT subtracted yet?
            // Let's check when the main bet is subtracted.
            // It's subtracted at the time of PlaceBet or NewRound logic.
            // In my Test setup, I just set playerHands = Hand(bet=100).
            // This hand has a bet, but the balance was 800.
            // So if I lose, balance stays 3400?
            // Only if the state machine doesn't subtract the bet of the hand.
            // Let's look at finalizeGame:
            // balance = current.balance + results.totalPayout
            // 3400 + 0 = 3400.
            // So 3400 is correct given my hand setup.
            assertEquals(3400, state.balance)
        }

    @Test
    fun sideBet_21Plus3_SuitedTriple_Win() =
        runTest {
            // Player: ACE_SPADES, ACE_SPADES. Dealer Upcard: ACE_SPADES.
            // Deck: ACE, ACE, ACE, TEN
            // 0:P, 1:D, 2:P, 3:D
            val deck =
                deckOf(
                    Rank.ACE, // P
                    Rank.ACE, // D (Upcard)
                    Rank.ACE, // P
                    Rank.TEN // D (Hole)
                )
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 800,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.TWENTY_ONE_PLUS_THREE to 100),
                    deck = deck,
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = sm.state.value
            // Initial 800. 21+3 win: 100 * 100 + 100 = 10100.
            // Total 10900.
            assertEquals(10900, state.balance)
            val sideResult = state.sideBetResults[SideBetType.TWENTY_ONE_PLUS_THREE]
            assertNotNull(sideResult)
            assertEquals("Suited Triple", sideResult.outcomeName)
        }

    @Test
    fun sideBet_Multiple_Wins_Sequential_Effects() =
        runTest {
            // Deck: ACE, ACE, ACE, TEN (P: ACE, ACE. D: ACE)
            // Index: 0:ACE(P), 1:ACE(D), 2:ACE(P), 3:TEN(D)
            val deck =
                deckOf(Rank.ACE, Rank.ACE, Rank.ACE, Rank.TEN)
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 800,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets =
                        persistentMapOf(
                            SideBetType.PERFECT_PAIRS to 100,
                            SideBetType.TWENTY_ONE_PLUS_THREE to 100
                        ),
                    deck = deck,
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.effects.test {
                sm.dispatch(GameAction.Deal)
                advanceUntilIdle()

                // Skip deal sounds
                repeat(4) { assertEquals(GameEffect.PlayCardSound, awaitItem()) }

                // Side bets resolved in applyInitialOutcome
                val results = mutableSetOf<SideBetType>()
                repeat(2) {
                    val effect = awaitItem() as GameEffect.ChipEruption
                    assertNotNull(effect.sideBetType, "Side bet type should not be null for side bet eruptions")
                    results.add(effect.sideBetType!!)
                }
                assertTrue(results.contains(SideBetType.PERFECT_PAIRS))
                assertTrue(results.contains(SideBetType.TWENTY_ONE_PLUS_THREE))

                assertEquals(GameEffect.PlayWinSound, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun sideBet_Persistence_Across_Rounds() =
        runTest {
            val deck = deckOf(Rank.FIVE, Rank.TEN, Rank.SIX, Rank.TEN)
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 1000,
                    playerHands = persistentListOf(Hand(bet = 100)),
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 50),
                    deck = deck,
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // After deal, sideBets moved to lastSideBets
            val state = sm.state.value
            assertEquals(50, state.lastSideBets[SideBetType.PERFECT_PAIRS])
            assertTrue(state.sideBets.isEmpty())

            // Start new round
            sm.dispatch(GameAction.NewGame())
            advanceUntilIdle()

            val newState = sm.state.value
            // Should be preserved and re-applied
            assertEquals(50, newState.lastSideBets[SideBetType.PERFECT_PAIRS])
            assertEquals(50, newState.sideBets[SideBetType.PERFECT_PAIRS])
        }

    @Test
    fun sideBet_ResetActions() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 800,
                    sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 100),
                    handCount = 1
                )
            val sm = testMachine(initialState)

            sm.dispatch(GameAction.ResetSideBets)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(900, state.balance)
            assertTrue(state.sideBets.isEmpty())
        }
}
