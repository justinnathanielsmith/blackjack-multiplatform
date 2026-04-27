package io.github.smithjustinn.blackjack.state

import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.logic.BettingActionOutcome
import io.github.smithjustinn.blackjack.logic.BettingLogic
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.logic.NewGameLogic
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.SideBetType

/**
 * State transitions for the pre-deal betting phase.
 *
 * This module routes betting actions to the [BettingLogic] domain layer and maps
 * the resulting [BettingActionOutcome] to a [ReducerResult].
 */

internal fun reduceNewGame(
    state: GameState,
    action: GameAction.NewGame
): ReducerResult {
    val resolvedBalance = action.initialBalance ?: state.balance
    val resolvedLastSideBets = if (action.lastSideBets.isEmpty()) state.lastSideBets else action.lastSideBets
    return ReducerResult(
        NewGameLogic.createInitialState(
            balance = resolvedBalance,
            rules = action.rules,
            handCount = action.handCount,
            previousBets = action.previousBets,
            lastSideBets = resolvedLastSideBets,
        )
    )
}

internal fun reduceDeal(state: GameState): ReducerResult = buildBettingResult(BettingLogic.deal(state))

internal fun reducePlaceBet(
    state: GameState,
    amount: Int,
    seatIndex: Int
): ReducerResult = buildBettingResult(BettingLogic.placeBet(state, amount, seatIndex))

internal fun reduceResetBet(
    state: GameState,
    seatIndex: Int?
): ReducerResult = buildBettingResult(BettingLogic.resetBet(state, seatIndex))

internal fun reduceSelectHandCount(
    state: GameState,
    count: Int
): ReducerResult = buildBettingResult(BettingLogic.selectHandCount(state, count))

internal fun reduceUpdateRules(
    state: GameState,
    rules: GameRules
): ReducerResult = buildBettingResult(BettingLogic.updateRules(state, rules))

internal fun reducePlaceSideBet(
    state: GameState,
    type: SideBetType,
    amount: Int
): ReducerResult = buildBettingResult(BettingLogic.placeSideBet(state, type, amount))

internal fun reduceResetSideBets(state: GameState): ReducerResult =
    buildBettingResult(BettingLogic.resetSideBets(state))

internal fun reduceResetSideBet(
    state: GameState,
    type: SideBetType
): ReducerResult = buildBettingResult(BettingLogic.resetSideBet(state, type))

/**
 * Converts a [BettingActionOutcome] into a [ReducerResult].
 *
 * If the outcome triggered the dealing sequence, it attaches a [ReducerCommand.RunDealSequence].
 */
private fun buildBettingResult(outcome: BettingActionOutcome): ReducerResult {
    val commands =
        if (outcome.isDealTriggered) {
            listOf(ReducerCommand.RunDealSequence)
        } else {
            emptyList()
        }
    return ReducerResult(
        state = outcome.state,
        effects = outcome.effects,
        commands = commands,
    )
}
