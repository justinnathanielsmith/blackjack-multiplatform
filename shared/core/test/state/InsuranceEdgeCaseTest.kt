@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state

import app.cash.turbine.test
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.testMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InsuranceEdgeCaseTest {
    // ── Gap 1: Double Blackjack ───────────────────────────────────────────────
    // NOTE: In production, resolveInitialOutcomeValues blocks INSURANCE_OFFERED when
    // anyPlayerHasBJ is true. These states are constructed directly to verify that the
    // reducer computes the correct balance when both sides have natural blackjack.

    @Test
    fun playerBJ_dealerBJ_tookInsurance_statusIsPushBalanceIsNetPositive() =
        runTest {
            // balance=900, bet=100
            // TakeInsurance: insuranceBet=50, balance=850; dealer score=21 → DEALER_TURN immediately
            // RevealDealerHole: 50*3=150 → balance=1000
            // FinalizeGame: player BJ vs dealer BJ → PUSH → +100 (bet returned) → balance=1100
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
                        playerHands = persistentListOf(hand(Rank.ACE, Rank.TEN).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1100, state.balance)
        }

    @Test
    fun playerBJ_dealerBJ_declinedInsurance_statusIsPushBalanceBreakEven() =
        runTest {
            // balance=900, bet=100
            // DeclineInsurance: no deduction; dealer score=21 → DEALER_TURN immediately
            // FinalizeGame: player BJ vs dealer BJ → PUSH → +100 → balance=1000
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
                        playerHands = persistentListOf(hand(Rank.ACE, Rank.TEN).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.DeclineInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance)
        }

    // ── Gap 2: Balance Boundary ───────────────────────────────────────────────

    @Test
    fun takeInsurance_balanceExactlyHalfBet_accepted_balanceBecomesZero() =
        runTest {
            // Guard: insuranceBet > balance (strictly greater); 50 > 50 = false → accepted
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 50,
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN), // score=18, not BJ → PLAYING
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.PLAYING, state.status)
            assertEquals(0, state.balance)
            assertEquals(50, state.insuranceBet)
        }

    @Test
    fun takeInsurance_balanceExactlyHalfBet_dealerBJ_insurancePaysBeyondZero() =
        runTest {
            // Enter mid-round: balance=0 (all spent on insurance), insuranceBet=50
            // Stand → dealer reveals BJ → RevealDealerHole: 50*3=150 → balance=150
            // FinalizeGame: player 11 vs dealer 21 → DEALER_WON, no main-bet payout
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 0,
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(150, state.balance)
        }

    @Test
    fun takeInsurance_balanceOneLessThanHalfBet_rejectedWithVibrate() =
        runTest {
            // insuranceBet=50, balance=49 → 50 > 49 = true → Vibrate, state unchanged
            val initialState =
                GameState(
                    status = GameStatus.INSURANCE_OFFERED,
                    balance = 49,
                    playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 100)),
                    dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
                    deck = persistentListOf(),
                )
            val sm = testMachine(initialState)

            sm.effects.test {
                sm.dispatch(GameAction.TakeInsurance)
                advanceUntilIdle()
                assertEquals(GameEffect.Vibrate, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(GameStatus.INSURANCE_OFFERED, sm.state.value.status)
            assertEquals(49, sm.state.value.balance)
        }

    // ── Gap 3: Multi-Hand (direct state, bypasses production gate) ────────────

    @Test
    fun multiHandDirectState_takeInsurance_usesFirstHandBetOnly() =
        runTest {
            // currentBet = playerHands[0].bet = 100 → insuranceBet = 100/2 = 50
            // Insurance is single-seat semantics: does NOT sum all hand bets (100+200=300 → /2=150)
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 800,
                        handCount = 2,
                        playerHands =
                            persistentListOf(
                                hand(Rank.FIVE, Rank.SIX).copy(bet = 100),
                                hand(Rank.SEVEN, Rank.EIGHT).copy(bet = 200),
                            ),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN), // not BJ → PLAYING
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(50, state.insuranceBet)
            assertEquals(750, state.balance) // 800 - 50
            assertEquals(GameStatus.PLAYING, state.status)
        }

    // ── Gap 4: Silent Effects ─────────────────────────────────────────────────

    @Test
    fun insuranceWin_dealerBJ_noChipEruptionEmittedForInsurancePortion() =
        runTest {
            // Insurance payout in RevealDealerHole is a silent balance update (no GameEffect emitted).
            // FinalizeGame: player 16 loses to dealer BJ → [ChipLoss, PlayLoseSound, Vibrate], no ChipEruption.
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.SIX).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(
                    emitted.any { it is GameEffect.ChipEruption },
                    "Insurance payout must be silent — no ChipEruption expected",
                )
                assertTrue(GameEffect.PlayLoseSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun insuranceLoss_dealerNoBJ_noChipLossEmittedForInsuranceSideBet() =
        runTest {
            // Insurance forfeiture has no matching effect; ChipLoss in FinalizeGame covers main bets only.
            // Player 19 beats dealer hard 17 → PLAYER_WON → [ChipEruption, PlayWinSound, WinPulse], no ChipLoss.
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.TEN, Rank.NINE).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN), // hard 17, stands without drawing
                        deck = persistentListOf(),
                    ),
                )

            sm.effects.test {
                sm.dispatch(GameAction.Stand)
                advanceUntilIdle()
                val emitted = buildList { repeat(3) { add(awaitItem()) } }
                assertFalse(
                    emitted.any { it is GameEffect.ChipLoss },
                    "Insurance forfeiture must be silent — no ChipLoss expected",
                )
                assertTrue(GameEffect.PlayWinSound in emitted)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun playerBJ_dealerBJ_insuranceTaken_emitsPlayPushSound() =
        runTest {
            // Double-BJ push: FinalizeGame status=PUSH → [ChipEruption(100), PlayPushSound]
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
                        insuranceBet = 50,
                        playerHands = persistentListOf(hand(Rank.ACE, Rank.TEN).copy(bet = 100)),
                        dealerHand = dealerHand(Rank.ACE, Rank.TEN),
                        deck = persistentListOf(),
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

    // ── Gap 5: Payout Math ────────────────────────────────────────────────────

    @Test
    fun insurancePayout_oddBet_integerDivisionRoundsDown() =
        runTest {
            // bet=101 → insuranceBet = 101/2 = 50 (not 51); rounding baked in at TakeInsurance time
            val sm =
                testMachine(
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 949,
                        playerHands = persistentListOf(hand(Rank.FIVE, Rank.SIX).copy(bet = 101)),
                        dealerHand = dealerHand(Rank.ACE, Rank.SEVEN), // not BJ → stays PLAYING
                        deck = persistentListOf(),
                    ),
                )

            sm.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = sm.state.value
            assertEquals(50, state.insuranceBet) // 101/2 = 50, not 51
            assertEquals(899, state.balance) // 949 - 50 = 899
        }
}
