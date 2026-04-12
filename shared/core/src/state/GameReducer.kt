package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.PlayerActionLogic
import io.github.smithjustinn.blackjack.logic.PlayerActionOutcome
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus

/**
 * A request for asynchronous orchestration emitted by the pure [reduce] function.
 *
 * **Functional Intent:**
 * - Offload time-sensitive operations (deals, dealer draw loops) to the middleware.
 * - Ensure the state machine's core reduction loop remains strictly non-suspending.
 * - Centralize sequence definitions while keeping transitions pure.
 */
internal sealed class ReducerCommand {
    /** Run the full card-by-card deal animation, then dispatch [GameAction.ApplyInitialOutcome]. */
    data object RunDealSequence : ReducerCommand()

    /** Run the dealer reveal-and-draw loop, then dispatch [GameAction.FinalizeGame]. */
    data object RunDealerTurn : ReducerCommand()
}

/**
 * The atomic output of a state transition.
 *
 * **Functional Intent:**
 * - [state]: The deterministic "Next State" of the game.
 * - [effects]: Fire-and-forget UI feedback (Auditory/Haptic).
 * - [commands]: Decoupled requests for temporal orchestration (Timing/Delays).
 *
 * Constraints: This carrier must be immutable and easily testable.
 */
internal data class ReducerResult(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
    val commands: List<ReducerCommand> = emptyList(),
)

// ── Pure Reducer ──────────────────────────────────────────────────────────────

/**
 * The pure state transition function (State + Action = Result).
 *
 * This function is the **Source of Truth** for all synchronous state mutations. It delegates
 * complex phase logic to specialized sub-reducers (Betting, Player, Internal).
 *
 * **Functional Intent:**
 * - Guarantee deterministic state transitions for any given action sequence.
 * - Prevent side effects (network, IO, delays) from leaking into the domain logic.
 * - Enable "Time Travel" debugging and exhaustive unit testing via pure inputs.
 *
 * Constraints: This function must NEVER suspend and must NEVER have side effects.
 */
internal fun reduce(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        // Grouped by phase to resolve CyclomaticComplexMethod violations in the main router.
        is GameAction.PlaceBet,
        is GameAction.ResetBet,
        is GameAction.ResetSeatBet,
        is GameAction.SelectHandCount,
        is GameAction.UpdateRules,
        is GameAction.PlaceSideBet,
        is GameAction.ResetSideBets,
        is GameAction.ResetSideBet -> routeBettingAction(state, action)

        is GameAction.NewGame,
        is GameAction.Deal -> routeLifecycleAction(state, action)

        is GameAction.Hit,
        is GameAction.Stand,
        is GameAction.DoubleDown,
        is GameAction.Split,
        is GameAction.Surrender,
        is GameAction.TakeInsurance,
        is GameAction.DeclineInsurance -> routePlayerAction(state, action)

        else -> routeInternalAction(state, action)
    }

private fun routeBettingAction(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        is GameAction.PlaceBet -> reducePlaceBet(state, action.amount, action.seatIndex)
        is GameAction.ResetBet -> reduceResetBet(state, seatIndex = null)
        is GameAction.ResetSeatBet -> reduceResetBet(state, seatIndex = action.seatIndex)
        is GameAction.SelectHandCount -> reduceSelectHandCount(state, action.count)
        is GameAction.UpdateRules -> reduceUpdateRules(state, action.rules)
        is GameAction.PlaceSideBet -> reducePlaceSideBet(state, action.type, action.amount)
        is GameAction.ResetSideBets -> reduceResetSideBets(state)
        is GameAction.ResetSideBet -> reduceResetSideBet(state, action.type)
        else -> ReducerResult(state)
    }

private fun routeLifecycleAction(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        is GameAction.NewGame -> reduceNewGame(state, action)
        is GameAction.Deal -> reduceDeal(state)
        else -> ReducerResult(state)
    }

private fun routePlayerAction(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        is GameAction.Hit -> buildPlayerActionResult(PlayerActionLogic.hit(state))
        is GameAction.Stand -> buildPlayerActionResult(PlayerActionLogic.stand(state))
        is GameAction.DoubleDown -> buildPlayerActionResult(PlayerActionLogic.doubleDown(state))
        is GameAction.Split -> buildPlayerActionResult(PlayerActionLogic.split(state))
        is GameAction.Surrender -> reduceSurrender(state)
        is GameAction.TakeInsurance -> reduceTakeInsurance(state)
        is GameAction.DeclineInsurance -> reduceDeclineInsurance(state)
        else -> ReducerResult(state)
    }

private fun routeInternalAction(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        is GameAction.SetDeck -> ReducerResult(state.copy(deck = action.deck))
        is GameAction.DealCardToPlayer -> reduceDealCardToPlayer(state, action.seatIndex)
        is GameAction.DealCardToDealer -> reduceDealCardToDealer(state, action.faceDown)
        is GameAction.ApplyInitialOutcome -> reduceApplyInitialOutcome(state)
        is GameAction.RevealDealerHole -> reduceRevealDealerHole(state)
        is GameAction.DealerDraw -> reduceDealerDraw(state)
        is GameAction.FinalizeGame -> reduceFinalizeGame(state)
        else -> ReducerResult(state)
    }

// ── Player action reducers ────────────────────────────────────────────────────

private fun reduceSurrender(state: GameState): ReducerResult {
    if (state.status != GameStatus.PLAYING ||
        state.activeHand.cards.size != 2 ||
        !state.rules.allowSurrender
    ) {
        return ReducerResult(state, listOf(GameEffect.Vibrate))
    }

    val refund = state.activeBet / 2
    val surrenderedHand = state.activeHand.copy(isSurrendered = true)
    val updatedHands = state.playerHands.set(state.activeHandIndex, surrenderedHand)
    val newState = state.copy(balance = state.balance + refund, playerHands = updatedHands)
    val effects = listOf(GameEffect.PlayLoseSound, GameEffect.ChipLoss(state.activeBet - refund))
    return buildPlayerActionResult(PlayerActionOutcome(state = newState, effects = effects, shouldAdvanceTurn = true))
}

private fun reduceTakeInsurance(state: GameState): ReducerResult {
    if (state.status != GameStatus.INSURANCE_OFFERED) return ReducerResult(state, listOf(GameEffect.Vibrate))
    val insuranceBet = state.currentBet / 2
    if (insuranceBet > state.balance) return ReducerResult(state, listOf(GameEffect.Vibrate))
    val newState =
        state.copy(
            balance = state.balance - insuranceBet,
            insuranceBet = insuranceBet,
        )
    // Hole card is still face-down but Hand.score counts all cards, so this correctly detects dealer BJ.
    return resolveInsuranceOutcome(newState)
}

private fun reduceDeclineInsurance(state: GameState): ReducerResult {
    if (state.status != GameStatus.INSURANCE_OFFERED) return ReducerResult(state, listOf(GameEffect.Vibrate))
    val newState = state.copy(insuranceBet = 0)
    return resolveInsuranceOutcome(newState)
}

// Insurance resolution: identical outcome path regardless of accept/decline — single source for the dealer-BJ check.
private fun resolveInsuranceOutcome(state: GameState): ReducerResult =
    if (state.dealerHand.score == BlackjackRules.BLACKJACK_SCORE) {
        ReducerResult(
            state = state.copy(status = GameStatus.DEALER_TURN),
            commands = listOf(ReducerCommand.RunDealerTurn),
        )
    } else {
        ReducerResult(state = state.copy(status = GameStatus.PLAYING))
    }

// ── Shared helper ─────────────────────────────────────────────────────────────

/**
 * Converts a [PlayerActionOutcome] into a [ReducerResult], handling multi-hand turn
 * advancement synchronously. This replaces the old `advanceOrEndTurn` suspend function.
 *
 * - If no turn advance is needed: returns the outcome's state + effects.
 * - If more player hands remain: increments [GameState.activeHandIndex].
 * - If the last hand is exhausted: transitions to [GameStatus.DEALER_TURN] and emits
 *   [ReducerCommand.RunDealerTurn] for the middleware to execute.
 */
private fun buildPlayerActionResult(outcome: PlayerActionOutcome): ReducerResult {
    if (!outcome.shouldAdvanceTurn) {
        return ReducerResult(outcome.state, outcome.effects)
    }
    return if (outcome.state.activeHandIndex < outcome.state.playerHands.size - 1) {
        ReducerResult(
            state = outcome.state.copy(activeHandIndex = outcome.state.activeHandIndex + 1),
            effects = outcome.effects,
        )
    } else {
        ReducerResult(
            state = outcome.state.copy(status = GameStatus.DEALER_TURN),
            effects = outcome.effects,
            commands = listOf(ReducerCommand.RunDealerTurn),
        )
    }
}
