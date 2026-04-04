package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

// ── Reducer return types ──────────────────────────────────────────────────────

/**
 * A command emitted by [reduce] requesting the middleware to perform an asynchronous
 * operation (e.g. an animated deal sequence or dealer draw loop). Commands are processed
 * outside the action loop so that the loop itself never suspends.
 */
internal sealed class ReducerCommand {
    /** Run the full card-by-card deal animation, then dispatch [GameAction.ApplyInitialOutcome]. */
    data object RunDealSequence : ReducerCommand()

    /** Run the dealer reveal-and-draw loop, then dispatch [GameAction.FinalizeGame]. */
    data object RunDealerTurn : ReducerCommand()
}

/**
 * The output of a single [reduce] invocation.
 *
 * @property state The next [GameState] produced synchronously.
 * @property effects UI-visible [GameEffect]s to emit (sounds, haptics, animations).
 * @property commands Middleware [ReducerCommand]s to execute asynchronously (carry delays).
 */
internal data class ReducerResult(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
    val commands: List<ReducerCommand> = emptyList(),
)

// ── Pure Reducer ──────────────────────────────────────────────────────────────

/**
 * The pure state transition function for the Blackjack game.
 *
 * Given a [GameState] and a [GameAction], returns the next [GameState], any [GameEffect]s
 * to emit, and any [ReducerCommand]s for the middleware layer to execute. This function
 * is 100% synchronous and has no side effects, making it trivially unit-testable without
 * coroutines or virtual time.
 */
internal fun reduce(
    state: GameState,
    action: GameAction
): ReducerResult =
    when (action) {
        // ── Betting phase ─────────────────────────────────────────────────────
        is GameAction.PlaceBet -> reducePlaceBet(state, action.amount, action.seatIndex)
        is GameAction.ResetBet -> reduceResetBet(state, seatIndex = null)
        is GameAction.ResetSeatBet -> reduceResetBet(state, seatIndex = action.seatIndex)
        is GameAction.SelectHandCount -> reduceSelectHandCount(state, action.count)
        is GameAction.UpdateRules -> reduceUpdateRules(state, action.rules)
        is GameAction.PlaceSideBet -> reducePlaceSideBet(state, action.type, action.amount)
        is GameAction.ResetSideBets -> reduceResetSideBets(state)
        is GameAction.ResetSideBet -> reduceResetSideBet(state, action.type)

        // ── Round lifecycle ───────────────────────────────────────────────────
        is GameAction.NewGame -> reduceNewGame(state, action)
        is GameAction.Deal -> reduceDeal(state)

        // ── Player actions ────────────────────────────────────────────────────
        is GameAction.Hit -> buildPlayerActionResult(PlayerActionLogic.hit(state))
        is GameAction.Stand -> buildPlayerActionResult(PlayerActionLogic.stand(state))
        is GameAction.DoubleDown -> buildPlayerActionResult(PlayerActionLogic.doubleDown(state))
        is GameAction.Split -> buildPlayerActionResult(PlayerActionLogic.split(state))
        is GameAction.Surrender -> reduceSurrender(state)
        is GameAction.TakeInsurance -> reduceTakeInsurance(state)
        is GameAction.DeclineInsurance -> reduceDeclineInsurance(state)

        // ── Internal primitives (dispatched by middleware) ────────────────────
        is GameAction.SetDeck -> ReducerResult(state.copy(deck = action.deck))
        is GameAction.DealCardToPlayer -> reduceDealCardToPlayer(state, action.seatIndex)
        is GameAction.DealCardToDealer -> reduceDealCardToDealer(state, action.faceDown)
        is GameAction.ApplyInitialOutcome -> reduceApplyInitialOutcome(state)
        is GameAction.RevealDealerHole -> reduceRevealDealerHole(state)
        is GameAction.DealerDraw -> reduceDealerDraw(state)
        is GameAction.FinalizeGame -> reduceFinalizeGame(state)
    }

// ── Betting reducers ──────────────────────────────────────────────────────────

private fun reducePlaceBet(
    state: GameState,
    amount: Int,
    seatIndex: Int
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    if (amount <= 0 || seatIndex !in 0 until state.handCount) {
        return ReducerResult(state, listOf(GameEffect.Vibrate))
    }
    if (amount > state.balance) {
        return ReducerResult(state, listOf(GameEffect.Vibrate))
    }
    val currentHand = state.playerHands[seatIndex]
    return ReducerResult(
        state.copy(
            balance = state.balance - amount,
            playerHands = state.playerHands.set(seatIndex, currentHand.copy(bet = currentHand.bet + amount)),
        )
    )
}

private fun reduceResetBet(
    state: GameState,
    seatIndex: Int?
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    return if (seatIndex == null) {
        // Bolt Performance Optimization: Replace .sumOf with indexed loop to avoid Iterator allocation.
        var refund = 0
        for (i in 0 until state.handCount) refund += state.playerHands[i].bet
        ReducerResult(
            state.copy(
                balance = state.balance + refund,
                playerHands =
                    state.playerHands.mutate { builder ->
                        for (i in 0 until state.handCount) builder[i] = builder[i].copy(bet = 0)
                    },
            )
        )
    } else {
        if (seatIndex !in 0 until state.handCount) return ReducerResult(state)
        val currentHand = state.playerHands[seatIndex]
        ReducerResult(
            state.copy(
                balance = state.balance + currentHand.bet,
                playerHands = state.playerHands.set(seatIndex, currentHand.copy(bet = 0)),
            )
        )
    }
}

private fun reduceSelectHandCount(
    state: GameState,
    count: Int
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    if (count !in
        BlackjackConfig.MIN_INITIAL_HANDS..BlackjackConfig.MAX_INITIAL_HANDS
    ) {
        return ReducerResult(state, listOf(GameEffect.Vibrate))
    }
    val delta = count - state.handCount
    if (delta == 0) return ReducerResult(state)
    val newHands: PersistentList<Hand>
    val balanceDelta: Int
    if (delta > 0) {
        newHands = state.playerHands.addAll(List(delta) { Hand() })
        balanceDelta = 0
    } else {
        // Bolt Performance Optimization: Replace .sumOf with indexed loop to avoid Iterator allocation.
        var refund = 0
        for (i in count until state.handCount) refund += state.playerHands.getOrNull(i)?.bet ?: 0
        newHands = state.playerHands.subList(0, count).toPersistentList()
        balanceDelta = refund
    }
    return ReducerResult(
        state.copy(handCount = count, playerHands = newHands, balance = state.balance + balanceDelta)
    )
}

private fun reduceUpdateRules(
    state: GameState,
    rules: GameRules
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    return ReducerResult(state.copy(rules = rules))
}

private fun reducePlaceSideBet(
    state: GameState,
    type: SideBetType,
    amount: Int
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    if (amount <= 0 || amount > state.balance) return ReducerResult(state)
    val newSideBets = state.sideBets.put(type, (state.sideBets[type] ?: 0) + amount)
    return ReducerResult(state.copy(balance = state.balance - amount, sideBets = newSideBets))
}

private fun reduceResetSideBets(state: GameState): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    var totalRefund = 0
    for ((_, betAmount) in state.sideBets) totalRefund += betAmount
    return ReducerResult(
        state.copy(balance = state.balance + totalRefund, sideBets = persistentMapOf())
    )
}

private fun reduceResetSideBet(
    state: GameState,
    type: SideBetType
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    val betAmount = state.sideBets[type] ?: return ReducerResult(state)
    return ReducerResult(
        state.copy(balance = state.balance + betAmount, sideBets = state.sideBets.remove(type))
    )
}

// ── Round lifecycle reducers ──────────────────────────────────────────────────

private fun reduceNewGame(
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

private fun reduceDeal(state: GameState): ReducerResult {
    if (state.status != GameStatus.BETTING ||
        state.playerHands.size != state.handCount ||
        state.playerHands.any { it.bet <= 0 }
    ) {
        return ReducerResult(state)
    }
    return ReducerResult(
        state = state.copy(status = GameStatus.DEALING),
        commands = listOf(ReducerCommand.RunDealSequence),
    )
}

// ── Player action reducers ────────────────────────────────────────────────────

private fun reduceSurrender(state: GameState): ReducerResult {
    if (state.status != GameStatus.PLAYING ||
        state.activeHand.cards.size != 2 ||
        !state.rules.allowSurrender
    ) {
        return ReducerResult(state)
    }

    val refund = state.activeBet / 2
    val surrenderedHand = state.activeHand.copy(isSurrendered = true)
    val updatedHands = state.playerHands.set(state.activeHandIndex, surrenderedHand)
    val newState = state.copy(balance = state.balance + refund, playerHands = updatedHands)
    val effects = listOf(GameEffect.PlayLoseSound, GameEffect.ChipLoss(state.activeBet - refund))
    return buildPlayerActionResult(PlayerActionOutcome(state = newState, effects = effects, shouldAdvanceTurn = true))
}

private fun reduceTakeInsurance(state: GameState): ReducerResult {
    if (state.status != GameStatus.INSURANCE_OFFERED) return ReducerResult(state)
    val insuranceBet = state.currentBet / 2
    if (insuranceBet > state.balance) return ReducerResult(state, listOf(GameEffect.Vibrate))
    val newState =
        state.copy(
            balance = state.balance - insuranceBet,
            insuranceBet = insuranceBet,
        )
    // Hole card is still face-down but Hand.score counts all cards, so this correctly detects dealer BJ.
    return if (newState.dealerHand.score == BlackjackRules.BLACKJACK_SCORE) {
        ReducerResult(
            state = newState.copy(status = GameStatus.DEALER_TURN),
            commands = listOf(ReducerCommand.RunDealerTurn),
        )
    } else {
        ReducerResult(state = newState.copy(status = GameStatus.PLAYING))
    }
}

private fun reduceDeclineInsurance(state: GameState): ReducerResult {
    if (state.status != GameStatus.INSURANCE_OFFERED) return ReducerResult(state)
    val newState = state.copy(insuranceBet = 0)
    return if (newState.dealerHand.score == BlackjackRules.BLACKJACK_SCORE) {
        ReducerResult(
            state = newState.copy(status = GameStatus.DEALER_TURN),
            commands = listOf(ReducerCommand.RunDealerTurn),
        )
    } else {
        ReducerResult(state = newState.copy(status = GameStatus.PLAYING))
    }
}

// ── Internal primitive reducers (called by middleware dispatch) ───────────────

private fun reduceDealCardToPlayer(
    state: GameState,
    seatIndex: Int
): ReducerResult {
    val card = state.deck.firstOrNull() ?: return ReducerResult(state)
    val newDeck = state.deck.removeAt(0)
    val hand = state.playerHands[seatIndex]
    val newHand = hand.copy(cards = hand.cards.add(card))
    return ReducerResult(
        state = state.copy(deck = newDeck, playerHands = state.playerHands.set(seatIndex, newHand)),
        effects = listOf(GameEffect.PlayCardSound),
    )
}

private fun reduceDealCardToDealer(
    state: GameState,
    faceDown: Boolean
): ReducerResult {
    val card = state.deck.firstOrNull() ?: return ReducerResult(state)
    val newDeck = state.deck.removeAt(0)
    val dealerCard = if (faceDown) card.copy(isFaceDown = true) else card
    val newDealerHand = state.dealerHand.copy(cards = state.dealerHand.cards.add(dealerCard))
    return ReducerResult(
        state = state.copy(deck = newDeck, dealerHand = newDealerHand),
        effects = listOf(GameEffect.PlayCardSound),
    )
}

private fun reduceApplyInitialOutcome(state: GameState): ReducerResult {
    val sideBetUpdate =
        SideBetLogic.resolveSideBets(
            sideBets = state.sideBets,
            playerHand = state.playerHands[0],
            dealerUpcard = state.dealerHand.cards[0],
        )
    val (initialStatus, finalDealerHand, balanceUpdate) =
        BlackjackRules.resolveInitialOutcomeValues(state, state.playerHands, state.dealerHand)
    val isMassiveSideBetWin = sideBetUpdate.results.values.any { it.payoutMultiplier >= 25 }

    val newState =
        state.copy(
            status = initialStatus,
            dealerHand = finalDealerHand,
            balance = state.balance + balanceUpdate + sideBetUpdate.payoutTotal,
            sideBetResults = sideBetUpdate.results,
            lastSideBets = state.sideBets,
            sideBets = persistentMapOf(),
        )

    val effects =
        buildList {
            // Juice: Always emit winning eruptions first.
            if (balanceUpdate > 0) add(GameEffect.ChipEruption(balanceUpdate))
            sideBetUpdate.results.forEach { (type, result) ->
                if (result.payoutAmount > 0) add(GameEffect.ChipEruption(result.payoutAmount, type))
            }
            // Then handle losses and sounds.
            state.sideBets.forEach { (type, amount) ->
                if (sideBetUpdate.results[type] == null) add(GameEffect.ChipLoss(amount))
            }
            when {
                initialStatus == GameStatus.PLAYER_WON || sideBetUpdate.payoutTotal > 0 -> {
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
    return ReducerResult(state = newState, effects = effects)
}

private fun reduceRevealDealerHole(state: GameState): ReducerResult {
    // Bolt Performance Optimization: Prevent reallocation of already face-up cards to preserve reference equality.
    val revealedCards =
        state.dealerHand.cards.mutate { builder ->
            for (i in 0 until builder.size) {
                val card = builder[i]
                if (card.isFaceDown) builder[i] = card.copy(isFaceDown = false)
            }
        }
    val revealedHand = state.dealerHand.copy(cards = revealedCards)

    val dealerHasNaturalBJ = revealedHand.isBlackjack
    val insurancePayout = if (state.insuranceBet > 0 && dealerHasNaturalBJ) state.insuranceBet * 3 else 0

    return ReducerResult(
        state =
            state.copy(
                dealerHand = revealedHand,
                balance = state.balance + insurancePayout,
            )
    )
}

private fun reduceDealerDraw(state: GameState): ReducerResult {
    val card = state.deck.firstOrNull() ?: return ReducerResult(state)
    val newDeck = state.deck.drop(1).toPersistentList()
    val isCritical =
        state.dealerHand.score in
            BlackjackRules.DEALER_STIFF_MIN until BlackjackRules.DEALER_STAND_THRESHOLD &&
            !state.dealerHand.isSoft
    val newDealerHand = state.dealerHand.copy(cards = state.dealerHand.cards.add(card))
    return ReducerResult(
        state =
            state.copy(
                deck = newDeck,
                dealerHand = newDealerHand,
                dealerDrawIsCritical = isCritical,
            ),
        effects = listOf(GameEffect.PlayCardSound),
    )
}

private fun reduceFinalizeGame(state: GameState): ReducerResult {
    val dealerScore = state.dealerHand.score
    val dealerBust = state.dealerHand.isBust
    val results = BlackjackRules.calculateHandResults(state, dealerScore, dealerBust)

    val finalStatus =
        when {
            results.anyWin -> GameStatus.PLAYER_WON
            results.allPush -> GameStatus.PUSH
            else -> GameStatus.DEALER_WON
        }
    val newState =
        state.copy(
            status = finalStatus,
            balance = state.balance + results.totalPayout,
            dealerDrawIsCritical = false,
        )

    val effects =
        buildList {
            if (results.totalPayout > 0) add(GameEffect.ChipEruption(results.totalPayout))
            // Bolt Performance Optimization: Replace .fold with indexed loop to avoid Iterator allocation.
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
    return ReducerResult(state = newState, effects = effects)
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
