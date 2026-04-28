package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.OutcomeEffectsLogic
import io.github.smithjustinn.blackjack.logic.SideBetLogic
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.HandOutcome
import io.github.smithjustinn.blackjack.model.isTerminal
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Reducers for **Engine Primitives** and internal state management.
 *
 * These functions handle the mechanical transitions of the game that are not
 * directly triggered by user input (e.g., automated card deals, dealer
 * draw loops, and final payout calculation).
 *
 * **Functional Intent:**
 * - **Middleware-Driven**: Most internal actions are dispatched by the
 *   [io.github.smithjustinn.blackjack.middleware.GameFlowMiddleware] to synchronize
 *   physical card movements with animation timing.
 * - **Finality**: Covers the transformation from active play to terminal
 *   statuses ([reduceFinalizeGame]).
 */

internal fun reduceDealCardToPlayer(
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

internal fun reduceDealCardToDealer(
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

internal fun reduceApplyInitialOutcome(state: GameState): ReducerResult {
    val sideBetUpdate =
        SideBetLogic.resolveSideBets(
            sideBets = state.sideBets,
            playerHand = state.playerHands[0],
            dealerUpcard = state.dealerHand.cards[0],
        )
    val outcome =
        BlackjackRules.resolveInitialOutcomeValues(state, state.playerHands, state.dealerHand)

    val settledHands =
        state.playerHands.mutate { builder ->
            for (i in 0 until builder.size) {
                val h = builder[i]
                if (h.isBlackjack) builder[i] = h.copy(isSettled = true)
            }
        }

    val netPayouts =
        BlackjackRules.calculateNetPayouts(
            playerHands = settledHands,
            dealerScore = outcome.dealerHand.score,
            dealerBust = outcome.dealerHand.isBust,
            rules = state.rules,
            isTerminal = outcome.status.isTerminal()
        )
    val totalNet = if (outcome.status.isTerminal()) netPayouts.sumOf { it ?: 0 } else null

    val newState =
        state.copy(
            status = outcome.status,
            dealerHand = outcome.dealerHand,
            playerHands = settledHands,
            balance = state.balance + outcome.balanceDelta + sideBetUpdate.payoutTotal,
            sideBetResults = sideBetUpdate.results,
            lastSideBets = state.sideBets,
            sideBets = persistentMapOf(),
            handNetPayouts = netPayouts,
            totalNetPayout = totalNet,
        )

    // Effect orchestration is a domain decision — see OutcomeEffectsLogic.
    val effects =
        OutcomeEffectsLogic.buildInitialOutcomeEffects(
            balanceUpdate = outcome.balanceDelta,
            sideBetUpdate = sideBetUpdate,
            state = state,
            initialStatus = outcome.status,
        )
    return ReducerResult(state = newState, effects = effects)
}

internal fun reduceRevealDealerHole(state: GameState): ReducerResult {
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

internal fun reduceDealerDraw(state: GameState): ReducerResult {
    val card = state.deck.firstOrNull() ?: return ReducerResult(state)
    val newDeck = state.deck.drop(1).toPersistentList()
    val isCritical = BlackjackRules.isDealerCriticalDraw(state.dealerHand)
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

internal fun reduceFinalizeGame(state: GameState): ReducerResult {
    val dealerScore = state.dealerHand.score
    val dealerBust = state.dealerHand.isBust
    val results = BlackjackRules.calculateHandResults(state, dealerScore, dealerBust)

    val finalStatus =
        when {
            results.anyWin -> GameStatus.PLAYER_WON
            results.allPush -> GameStatus.PUSH
            else -> GameStatus.DEALER_WON
        }

    val outcomes =
        state.playerHands
            .map { hand ->
                if (hand.isSurrendered) {
                    HandOutcome.LOSS
                } else {
                    BlackjackRules.determineHandOutcome(hand, dealerScore, dealerBust)
                }
            }.toPersistentList()

    val newState =
        state.copy(
            status = finalStatus,
            balance = state.balance + results.totalPayout,
            dealerDrawIsCritical = false,
            handOutcomes = outcomes,
            handNetPayouts = results.netPayouts,
            totalNetPayout = results.netPayouts.sumOf { it ?: 0 },
        )

    // Effect orchestration is a domain decision — see OutcomeEffectsLogic.
    val effects = OutcomeEffectsLogic.buildFinalizeEffects(results, state, finalStatus)
    return ReducerResult(state = newState, effects = effects)
}
