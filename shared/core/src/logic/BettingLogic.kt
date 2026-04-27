package io.github.smithjustinn.blackjack.logic

import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.SideBetType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList

/**
 * Represents the result of a betting action processed by [BettingLogic].
 *
 * @property state The next [GameState] produced by the action.
 * @property effects Auditory and visual [GameEffect]s to be emitted by the UI (e.g., plink sounds).
 * @property isDealTriggered True if the action should trigger the dealing sequence.
 */
data class BettingActionOutcome(
    val state: GameState,
    val effects: List<GameEffect> = emptyList(),
    val isDealTriggered: Boolean = false
) {
    companion object {
        /**
         * Creates a [BettingActionOutcome] that returns the provided [state] without any
         * changes or side effects.
         */
        fun noop(state: GameState): BettingActionOutcome = BettingActionOutcome(state)

        /**
         * Creates a [BettingActionOutcome] that returns the provided [state] with a
         * [GameEffect.Vibrate] effect. Used for actions that are rejected due to
         * status guards or rule violations.
         */
        fun rejected(state: GameState): BettingActionOutcome =
            BettingActionOutcome(state, effects = listOf(GameEffect.Vibrate))
    }
}

/**
 * The core engine for player betting logic in Blackjack.
 *
 * This implementation handles placing bets, managing side bets, selecting hand counts,
 * and transitioning to the dealing phase. It encapsulates:
 * 1. **Balance Protection**: Strict guards against exceeding the available bankroll.
 * 2. **Bet Idempotency**: Ensuring the table can be cleared safely.
 * 3. **Juice & Feedback**: Selection and emission of relevant [GameEffect]s for a premium feel.
 *
 * All functions are pure state transitions that return a [BettingActionOutcome].
 */
object BettingLogic {
    fun deal(state: GameState): BettingActionOutcome {
        if (state.status != GameStatus.BETTING ||
            state.playerHands.size != state.handCount ||
            state.playerHands.any { it.bet <= 0 }
        ) {
            return BettingActionOutcome.rejected(state)
        }
        val capturedBets = state.playerHands.map { it.bet }.toPersistentList()
        return BettingActionOutcome(
            state =
                state.copy(
                    status = GameStatus.DEALING,
                    lastBets = capturedBets
                ),
            isDealTriggered = true
        )
    }

    fun placeBet(
        state: GameState,
        amount: Int,
        seatIndex: Int
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        if (amount <= 0 || seatIndex !in 0 until state.handCount) {
            return BettingActionOutcome.rejected(state)
        }
        if (amount > state.balance) {
            return BettingActionOutcome.rejected(state)
        }
        val currentHand = state.playerHands[seatIndex]
        return BettingActionOutcome(
            state =
                state.copy(
                    balance = state.balance - amount,
                    playerHands = state.playerHands.set(seatIndex, currentHand.copy(bet = currentHand.bet + amount)),
                ),
            effects = listOf(GameEffect.PlayPlinkSound)
        )
    }

    fun resetBet(
        state: GameState,
        seatIndex: Int?
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        return if (seatIndex == null) {
            var refund = 0
            for (i in 0 until state.handCount) refund += state.playerHands[i].bet
            BettingActionOutcome(
                state =
                    state.copy(
                        balance = state.balance + refund,
                        playerHands =
                            state.playerHands.mutate { builder ->
                                for (i in 0 until state.handCount) builder[i] = builder[i].copy(bet = 0)
                            },
                    ),
                effects = listOf(GameEffect.PlayPlinkSound)
            )
        } else {
            if (seatIndex !in 0 until state.handCount) return BettingActionOutcome.noop(state)
            val currentHand = state.playerHands[seatIndex]
            BettingActionOutcome(
                state =
                    state.copy(
                        balance = state.balance + currentHand.bet,
                        playerHands = state.playerHands.set(seatIndex, currentHand.copy(bet = 0)),
                    ),
                effects = listOf(GameEffect.PlayPlinkSound)
            )
        }
    }

    fun selectHandCount(
        state: GameState,
        count: Int
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        if (count !in BlackjackConfig.MIN_INITIAL_HANDS..BlackjackConfig.MAX_INITIAL_HANDS) {
            return BettingActionOutcome.rejected(state)
        }
        val delta = count - state.handCount
        if (delta == 0) return BettingActionOutcome.noop(state)
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
        return BettingActionOutcome(
            state =
                state.copy(
                    handCount = count,
                    playerHands = newHands,
                    balance = state.balance + balanceDelta
                )
        )
    }

    fun updateRules(
        state: GameState,
        rules: GameRules
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        return BettingActionOutcome(state = state.copy(rules = rules))
    }

    fun placeSideBet(
        state: GameState,
        type: SideBetType,
        amount: Int
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        if (amount <= 0 || amount > state.balance) return BettingActionOutcome.rejected(state)
        val newSideBets = state.sideBets.put(type, (state.sideBets[type] ?: 0) + amount)
        return BettingActionOutcome(
            state = state.copy(balance = state.balance - amount, sideBets = newSideBets),
            effects = listOf(GameEffect.PlayPlinkSound)
        )
    }

    fun resetSideBets(state: GameState): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        var totalRefund = 0
        for ((_, betAmount) in state.sideBets) totalRefund += betAmount
        return BettingActionOutcome(
            state = state.copy(balance = state.balance + totalRefund, sideBets = persistentMapOf()),
            effects = listOf(GameEffect.PlayPlinkSound)
        )
    }

    fun resetSideBet(
        state: GameState,
        type: SideBetType
    ): BettingActionOutcome {
        if (state.status != GameStatus.BETTING) return BettingActionOutcome.rejected(state)
        val betAmount = state.sideBets[type] ?: return BettingActionOutcome.rejected(state)
        return BettingActionOutcome(
            state = state.copy(balance = state.balance + betAmount, sideBets = state.sideBets.remove(type)),
            effects = listOf(GameEffect.PlayPlinkSound)
        )
    }
}
