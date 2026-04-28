package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.HandResults

/**
 * Pure domain rules deciding which [GameEffect]s accompany a round-outcome transition —
 * loss-vibration policy, massive-win threshold, win/lose/push branching,
 * aggregate bet-vs-payout chip animations, and per-side-bet attribution.
 *
 * Extracted from `InternalReducer` so the routing layer only routes; the rules
 * about *what feedback to emit* live alongside [BlackjackRules] and [SideBetLogic].
 */
object OutcomeEffectsLogic {
    /**
     * Builds the post-finalize feedback list for a fully resolved round.
     *
     * Rules encoded:
     * - [GameEffect.ChipEruption] when payout > 0; [GameEffect.ChipLoss] when payout < total bet.
     * - On [GameStatus.PLAYER_WON]: win sound + win pulse.
     * - On [GameStatus.DEALER_WON]: lose sound; [GameEffect.Vibrate] **only** if no hand busted
     *   (busts already played a [GameEffect.BustThud]; vibration would double-up).
     * - On [GameStatus.PUSH]: push sound.
     */
    fun buildFinalizeEffects(
        results: HandResults,
        state: GameState,
        finalStatus: GameStatus,
    ): List<GameEffect> =
        buildList {
            if (results.totalPayout > 0) add(GameEffect.ChipEruption(results.totalPayout))
            var totalBet = 0
            for (i in 0 until state.playerHands.size) totalBet += state.playerHands[i].bet
            if (results.totalPayout < totalBet) add(GameEffect.ChipLoss(totalBet - results.totalPayout))
            when (finalStatus) {
                GameStatus.PLAYER_WON -> {
                    add(GameEffect.PlayWinSound)
                    add(GameEffect.WinPulse)
                }
                GameStatus.DEALER_WON -> {
                    add(GameEffect.PlayLoseSound)
                    if (state.playerHands.none { it.isBust }) add(GameEffect.Vibrate)
                }
                GameStatus.PUSH -> add(GameEffect.PlayPushSound)
                else -> {}
            }
        }

    /**
     * Builds the feedback list for the *initial* outcome resolution (immediately after the deal,
     * before any player actions). Side-bet payouts are attributed individually so the UI can
     * animate per-bet eruptions; lost side bets each emit their own [GameEffect.ChipLoss].
     *
     * The massive-win threshold ([BlackjackConfig.MASSIVE_WIN_MULTIPLIER]) elevates a side-bet
     * win to [GameEffect.BigWin] in lieu of [GameEffect.PlayWinSound] so the orchestrator can
     * play the THE_NUTS audio + banner.
     */
    fun buildInitialOutcomeEffects(
        balanceUpdate: Int,
        sideBetUpdate: SideBetResolution,
        state: GameState,
        initialStatus: GameStatus,
    ): List<GameEffect> {
        val isMassiveSideBetWin =
            sideBetUpdate.results.values.any { it.payoutMultiplier >= BlackjackConfig.MASSIVE_WIN_MULTIPLIER }
        return buildList {
            // Juice: Always emit winning eruptions first.
            if (balanceUpdate > 0) add(GameEffect.ChipEruption(balanceUpdate))
            sideBetUpdate.results.forEach { (type, result) ->
                if (result.payoutAmount > 0) add(GameEffect.ChipEruption(result.payoutAmount, type))
            }
            // Then handle losses and sounds.
            state.sideBets.forEach { (type, amount) ->
                if (sideBetUpdate.results[type] == null) add(GameEffect.ChipLoss(amount))
            }
            val isWinOutcome = initialStatus == GameStatus.PLAYER_WON || sideBetUpdate.payoutTotal > 0
            when {
                isWinOutcome -> {
                    if (isMassiveSideBetWin) {
                        add(GameEffect.BigWin(sideBetUpdate.payoutTotal))
                    } else {
                        add(GameEffect.PlayWinSound)
                    }
                    if (initialStatus == GameStatus.PLAYER_WON) add(GameEffect.WinPulse)
                }
                initialStatus == GameStatus.DEALER_WON -> {
                    add(GameEffect.PlayLoseSound)
                    add(GameEffect.ChipLoss(state.currentBet))
                }
                initialStatus == GameStatus.PUSH -> add(GameEffect.PlayPushSound)
            }
        }
    }
}
