package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.SideBetLogic
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.HandOutcome
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Internal logic for the blackjack engine, typically called via middleware dispatch.
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
    val (initialStatus, finalDealerHand, balanceUpdate) =
        BlackjackRules.resolveInitialOutcomeValues(state, state.playerHands, state.dealerHand)
    val isMassiveSideBetWin =
        sideBetUpdate.results.values.any { it.payoutMultiplier >= BlackjackConfig.MASSIVE_WIN_MULTIPLIER }

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
        getEffectsForApplyInitialOutcome(
            balanceUpdate = balanceUpdate,
            sideBetUpdate = sideBetUpdate,
            state = state,
            initialStatus = initialStatus,
            isMassiveSideBetWin = isMassiveSideBetWin
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
        )

    val effects = getEffectsForFinalizeGame(results, state, finalStatus)
    return ReducerResult(state = newState, effects = effects)
}

private fun getEffectsForFinalizeGame(
    results: io.github.smithjustinn.blackjack.model.HandResults,
    state: GameState,
    finalStatus: GameStatus
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

private fun getEffectsForApplyInitialOutcome(
    balanceUpdate: Int,
    sideBetUpdate: io.github.smithjustinn.blackjack.logic.SideBetResolution,
    state: GameState,
    initialStatus: GameStatus,
    isMassiveSideBetWin: Boolean
): List<GameEffect> =
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
