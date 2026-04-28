package io.github.smithjustinn.blackjack.logic

import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.HandResults
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.SideBetOutcome
import io.github.smithjustinn.blackjack.model.SideBetResult
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.util.dealerHand
import io.github.smithjustinn.blackjack.util.hand
import io.github.smithjustinn.blackjack.util.playingState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OutcomeEffectsLogicTest {
    // ── buildFinalizeEffects ──────────────────────────────────────────────────

    @Test
    fun buildFinalizeEffects_playerWon_emitsWinSoundAndWinPulse() {
        val state = playingState(playerHand = hand(Rank.TEN, Rank.NINE), dealerHand = dealerHand(Rank.TEN, Rank.SIX))
        val results =
            HandResults(
                totalPayout = 200,
                anyWin = true,
                allPush = false,
                netPayouts = persistentListOf(100),
            )

        val effects = OutcomeEffectsLogic.buildFinalizeEffects(results, state, GameStatus.PLAYER_WON)

        assertEquals(GameEffect.ChipEruption(200), effects.first())
        assertContains(effects, GameEffect.PlayWinSound)
        assertContains(effects, GameEffect.WinPulse)
        assertFalse(GameEffect.Vibrate in effects)
    }

    @Test
    fun buildFinalizeEffects_dealerWon_noBust_emitsVibrate() {
        // Player stood on 19, dealer hit 20 — player did NOT bust, vibration applies.
        val state = playingState(playerHand = hand(Rank.TEN, Rank.NINE), dealerHand = dealerHand(Rank.TEN, Rank.TEN))
        val results =
            HandResults(
                totalPayout = 0,
                anyWin = false,
                allPush = false,
                netPayouts = persistentListOf(-100),
            )

        val effects = OutcomeEffectsLogic.buildFinalizeEffects(results, state, GameStatus.DEALER_WON)

        assertContains(effects, GameEffect.PlayLoseSound)
        assertContains(effects, GameEffect.Vibrate)
        assertContains(effects, GameEffect.ChipLoss(100))
    }

    @Test
    fun buildFinalizeEffects_dealerWon_playerBust_suppressesVibrate() {
        // Player busted (22) — BustThud already played, suppress duplicate Vibrate.
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.NINE, Rank.FIVE), // 24 → bust
                dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
            )
        val results =
            HandResults(
                totalPayout = 0,
                anyWin = false,
                allPush = false,
                netPayouts = persistentListOf(-100),
            )

        val effects = OutcomeEffectsLogic.buildFinalizeEffects(results, state, GameStatus.DEALER_WON)

        assertContains(effects, GameEffect.PlayLoseSound)
        assertFalse(GameEffect.Vibrate in effects)
    }

    @Test
    fun buildFinalizeEffects_push_emitsPushSound_noChipMovement() {
        val state = playingState(playerHand = hand(Rank.TEN, Rank.NINE), dealerHand = dealerHand(Rank.TEN, Rank.NINE))
        val results =
            HandResults(
                totalPayout = 100, // stake returned
                anyWin = false,
                allPush = true,
                netPayouts = persistentListOf(0),
            )

        val effects = OutcomeEffectsLogic.buildFinalizeEffects(results, state, GameStatus.PUSH)

        assertContains(effects, GameEffect.PlayPushSound)
        assertContains(effects, GameEffect.ChipEruption(100))
        // totalPayout == totalBet → no ChipLoss
        assertTrue(effects.none { it is GameEffect.ChipLoss })
    }

    // ── buildInitialOutcomeEffects ────────────────────────────────────────────

    @Test
    fun buildInitialOutcomeEffects_massiveSideWin_emitsBigWin_notWinSound() {
        val state =
            playingState(
                playerHand = hand(Rank.ACE, Rank.ACE),
                dealerHand = dealerHand(Rank.ACE, Rank.SEVEN),
            ).copy(sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 25))
        val sideBetUpdate =
            SideBetResolution(
                payoutTotal = 650,
                results =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to
                            SideBetResult(
                                type = SideBetType.PERFECT_PAIRS,
                                payoutMultiplier = BlackjackConfig.MASSIVE_WIN_MULTIPLIER,
                                payoutAmount = 650,
                                outcome = SideBetOutcome.PERFECT_PAIR,
                            ),
                    ),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = sideBetUpdate,
                state = state,
                initialStatus = GameStatus.PLAYING,
            )

        assertContains(effects, GameEffect.BigWin(650))
        assertFalse(GameEffect.PlayWinSound in effects)
    }

    @Test
    fun buildInitialOutcomeEffects_belowMassiveThreshold_emitsWinSound() {
        val state =
            playingState(
                playerHand = hand(Rank.SEVEN, Rank.SEVEN),
                dealerHand = dealerHand(Rank.NINE, Rank.SIX),
            ).copy(sideBets = persistentMapOf(SideBetType.PERFECT_PAIRS to 10))
        val sideBetUpdate =
            SideBetResolution(
                payoutTotal = 50,
                results =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to
                            SideBetResult(
                                type = SideBetType.PERFECT_PAIRS,
                                payoutMultiplier = 5,
                                payoutAmount = 50,
                                outcome = SideBetOutcome.MIXED_PAIR,
                            ),
                    ),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = sideBetUpdate,
                state = state,
                initialStatus = GameStatus.PLAYING,
            )

        assertContains(effects, GameEffect.PlayWinSound)
        assertFalse(effects.any { it is GameEffect.BigWin })
        assertContains(effects, GameEffect.ChipEruption(50, SideBetType.PERFECT_PAIRS))
    }

    @Test
    fun buildInitialOutcomeEffects_playerWonOnDeal_addsWinPulse() {
        val state =
            playingState(
                playerHand = hand(Rank.ACE, Rank.KING), // natural BJ
                dealerHand = dealerHand(Rank.NINE, Rank.SIX),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 250,
                sideBetUpdate = SideBetResolution(payoutTotal = 0, results = persistentMapOf()),
                state = state,
                initialStatus = GameStatus.PLAYER_WON,
            )

        assertEquals(GameEffect.ChipEruption(250), effects.first())
        assertContains(effects, GameEffect.PlayWinSound)
        assertContains(effects, GameEffect.WinPulse)
    }

    @Test
    fun buildInitialOutcomeEffects_dealerWon_emitsLoseSoundAndChipLoss() {
        val state =
            playingState(
                playerHand = hand(Rank.NINE, Rank.SEVEN),
                dealerHand = dealerHand(Rank.ACE, Rank.KING),
                bet = 75,
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = SideBetResolution(payoutTotal = 0, results = persistentMapOf()),
                state = state,
                initialStatus = GameStatus.DEALER_WON,
            )

        assertContains(effects, GameEffect.PlayLoseSound)
        assertContains(effects, GameEffect.ChipLoss(75))
    }

    @Test
    fun buildInitialOutcomeEffects_lostSideBets_emitChipLossPerBet() {
        // Two side bets placed; neither resolved (i.e., both lost).
        val state =
            playingState(
                playerHand = hand(Rank.NINE, Rank.SEVEN),
                dealerHand = dealerHand(Rank.NINE, Rank.SIX),
            ).copy(
                sideBets =
                    persistentMapOf(
                        SideBetType.PERFECT_PAIRS to 10,
                        SideBetType.TWENTY_ONE_PLUS_THREE to 20,
                    ),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = SideBetResolution(payoutTotal = 0, results = persistentMapOf()),
                state = state,
                initialStatus = GameStatus.PLAYING,
            )

        assertContains(effects, GameEffect.ChipLoss(10))
        assertContains(effects, GameEffect.ChipLoss(20))
    }

    @Test
    fun buildInitialOutcomeEffects_push_emitsPushSound() {
        val state =
            playingState(
                playerHand = hand(Rank.TEN, Rank.NINE),
                dealerHand = dealerHand(Rank.TEN, Rank.NINE),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = SideBetResolution(payoutTotal = 0, results = persistentMapOf()),
                state = state,
                initialStatus = GameStatus.PUSH,
            )

        assertContains(effects, GameEffect.PlayPushSound)
        assertFalse(effects.any { it is GameEffect.ChipLoss })
    }

    @Test
    fun buildInitialOutcomeEffects_emptyState_returnsEmptyList() {
        // No win, no loss, no push — e.g., status stays PLAYING (no immediate outcome).
        val state =
            GameState(
                status = GameStatus.PLAYING,
                playerHands = listOf(Hand().copy(bet = 50)).toPersistentList(),
            )

        val effects =
            OutcomeEffectsLogic.buildInitialOutcomeEffects(
                balanceUpdate = 0,
                sideBetUpdate = SideBetResolution(payoutTotal = 0, results = persistentMapOf()),
                state = state,
                initialStatus = GameStatus.PLAYING,
            )

        assertTrue(effects.isEmpty())
    }
}
