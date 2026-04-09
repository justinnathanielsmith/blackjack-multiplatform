package io.github.smithjustinn.blackjack.middleware
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.state.ReducerCommand
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

/**
 * Executes asynchronous [ReducerCommand]s emitted by the pure reducer.
 *
 * This class owns all animation timing and sequencing logic, keeping [BlackjackStateMachine]
 * free of delay and orchestration concerns. It communicates back to the state machine
 * exclusively through the [dispatch] callback.
 *
 * @param state Read-only view of the current [GameState].
 * @param dispatch Callback used to send [GameAction]s back to the state machine.
 * @param emitEffect Callback used to emit fire-and-forget [GameEffect]s (e.g. sounds).
 * @param isTest When true, all delays collapse to 0 ms for fast, deterministic tests.
 * @param logger Logger instance.
 */
internal class GameFlowMiddleware(
    private val state: StateFlow<GameState>,
    private val dispatch: suspend (GameAction) -> Unit,
    private val emitEffect: (GameEffect) -> Unit,
    private val isTest: Boolean,
    private val logger: Logger,
) {
    companion object {
        private const val DEALER_TURN_DELAY_MS = 600L
        private const val DEAL_CARD_DELAY_MS = 400L
        private const val DEALER_CRITICAL_PRE_DELAY_MS = 900L
        private const val REVEAL_DELAY_MS = 1500L
        private const val SLOW_ROLL_DELAY_MS = 2500L
    }

    private val dealCardDelayMs: Long get() = if (isTest) 0L else DEAL_CARD_DELAY_MS
    private val dealerTurnDelayMs: Long get() = if (isTest) 0L else DEALER_TURN_DELAY_MS
    private val dealerCriticalPreDelayMs: Long get() = if (isTest) 0L else DEALER_CRITICAL_PRE_DELAY_MS

    /**
     * Executes the given [ReducerCommand], suspending until it completes.
     *
     * Called serially by [BlackjackStateMachine]'s command loop — only one command runs at a time.
     */
    suspend fun execute(cmd: ReducerCommand) {
        when (cmd) {
            is ReducerCommand.RunDealSequence -> executeRunDealSequence()
            is ReducerCommand.RunDealerTurn -> executeRunDealerTurn()
        }
    }

    /**
     * Orchestrates the card-by-card deal animation.
     *
     * Computes a fresh deck if needed, then dispatches [GameAction.DealCardToPlayer] and
     * [GameAction.DealCardToDealer] interleaved with [delay]s. Finishes by dispatching
     * [GameAction.ApplyInitialOutcome] after an optional reveal delay.
     */
    private suspend fun executeRunDealSequence() {
        val current = state.value
        val freshDeck = getDeck(current)
        if (freshDeck !== current.deck) {
            dispatch(GameAction.SetDeck(freshDeck.toPersistentList()))
        }

        // Interleaved deal rounds: (P0..Pn, Dealer) × 2, second dealer card is face-down.
        for (round in 0..1) {
            val handCount = state.value.handCount
            for (i in 0 until handCount) {
                delay(dealCardDelayMs)
                dispatch(GameAction.DealCardToPlayer(i))
            }
            delay(dealCardDelayMs)
            dispatch(GameAction.DealCardToDealer(faceDown = round == 1))
        }

        delay(getRevealDelayMs(state.value.dealerHand))
        dispatch(GameAction.ApplyInitialOutcome)
    }

    /**
     * Orchestrates the dealer reveal and draw loop.
     *
     * Dispatches [GameAction.RevealDealerHole], waits for the reveal delay, then loops
     * drawing cards (with optional critical-draw tension) until the dealer stands.
     * Finishes by dispatching [GameAction.FinalizeGame].
     */
    private suspend fun executeRunDealerTurn() {
        dispatch(GameAction.RevealDealerHole)
        delay(getRevealDelayMs(state.value.dealerHand))

        while (BlackjackRules.shouldDealerDraw(state.value.dealerHand, state.value.rules)) {
            if (state.value.deck.isEmpty()) break // safety valve

            val hand = state.value.dealerHand
            val isCritical =
                hand.score in
                    BlackjackRules.DEALER_STIFF_MIN until BlackjackRules.DEALER_STAND_THRESHOLD &&
                    !hand.isSoft

            if (isCritical) {
                // Emit DealerCriticalDraw BEFORE the card draw so the effect precedes PlayCardSound.
                emitEffect(GameEffect.DealerCriticalDraw)
                delay(dealerCriticalPreDelayMs)
            }

            dispatch(GameAction.DealerDraw)
            delay(dealerTurnDelayMs)
        }

        dispatch(GameAction.FinalizeGame)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun getDeck(current: GameState): List<Card> {
        val totalCards = current.rules.deckCount * BlackjackRules.CARDS_PER_DECK
        val threshold = totalCards / BlackjackRules.RESHUFFLE_THRESHOLD_DIVISOR

        // In test mode, only reshuffle when strictly empty to preserve deterministic sequences.
        // In production, reshuffle at the threshold.
        val shouldReshuffle = current.deck.isEmpty() || (current.deck.size <= threshold && !isTest)

        return if (shouldReshuffle) {
            BlackjackRules.createDeck(current.rules)
        } else {
            current.deck
        }
    }

    private fun getRevealDelayMs(hand: Hand): Long {
        if (isTest) return 0L
        return if (hand.isBlackjack) SLOW_ROLL_DELAY_MS else REVEAL_DELAY_MS
    }
}
