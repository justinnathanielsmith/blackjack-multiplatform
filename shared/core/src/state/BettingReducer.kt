package io.github.smithjustinn.blackjack.state
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

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

internal fun reduceDeal(state: GameState): ReducerResult {
    if (state.status != GameStatus.BETTING ||
        state.playerHands.size != state.handCount ||
        state.playerHands.any { it.bet <= 0 }
    ) {
        return ReducerResult(state)
    }
    val capturedBets = state.playerHands.map { it.bet }.toPersistentList()
    return ReducerResult(
        state =
            state.copy(
                status = GameStatus.DEALING,
                lastBets = capturedBets
            ),
        commands = listOf(ReducerCommand.RunDealSequence),
    )
}

internal fun reducePlaceBet(
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

internal fun reduceResetBet(
    state: GameState,
    seatIndex: Int?
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    return if (seatIndex == null) {
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

internal fun reduceSelectHandCount(
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
        var refund = 0
        for (i in count until state.handCount) refund += state.playerHands.getOrNull(i)?.bet ?: 0
        newHands = state.playerHands.subList(0, count).toPersistentList()
        balanceDelta = refund
    }
    return ReducerResult(
        state.copy(handCount = count, playerHands = newHands, balance = state.balance + balanceDelta)
    )
}

internal fun reduceUpdateRules(
    state: GameState,
    rules: GameRules
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    return ReducerResult(state.copy(rules = rules))
}

internal fun reducePlaceSideBet(
    state: GameState,
    type: SideBetType,
    amount: Int
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    if (amount <= 0 || amount > state.balance) return ReducerResult(state)
    val newSideBets = state.sideBets.put(type, (state.sideBets[type] ?: 0) + amount)
    return ReducerResult(state.copy(balance = state.balance - amount, sideBets = newSideBets))
}

internal fun reduceResetSideBets(state: GameState): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    var totalRefund = 0
    for ((_, betAmount) in state.sideBets) totalRefund += betAmount
    return ReducerResult(
        state.copy(balance = state.balance + totalRefund, sideBets = persistentMapOf())
    )
}

internal fun reduceResetSideBet(
    state: GameState,
    type: SideBetType
): ReducerResult {
    if (state.status != GameStatus.BETTING) return ReducerResult(state)
    val betAmount = state.sideBets[type] ?: return ReducerResult(state)
    return ReducerResult(
        state.copy(balance = state.balance + betAmount, sideBets = state.sideBets.remove(type))
    )
}
