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

data class GameFlowConfig(
    val dealerTurnDelayMs: Long = 600L,
    val dealCardDelayMs: Long = 400L,
    val dealerCriticalPreDelayMs: Long = 900L,
    val revealDelayMs: Long = 1500L,
    val slowRollDelayMs: Long = 2500L
)

/**
 * The **Temporal Orchestrator** for the Blackjack engine.
 *
 * This middleware manages the asynchronous sequencing of game events that require
 * physical card timing, animations, or artificial delays (e.g., dealer thinking time).
 *
 * **Functional Intent:**
 * - **Decoupled Timing**: Keeps the [io.github.smithjustinn.blackjack.state.DefaultBlackjackStateMachine]
 *   strictly non-suspending by offloading all `delay()` calls to this async layer.
 * - **Serial Execution**: Processes [ReducerCommand]s one at a time via a dedicated
 *   internal loop, preventing overlapping animations (e.g., dealer drawing while
 *   dealing is still in progress).
 * - **State Feedback**: Communicates back to the engine exclusively via [dispatch].
 *
 * Constraints: This class is internal and should only be instantiated by the State Machine.
 */
internal class GameFlowMiddleware(
    private val state: StateFlow<GameState>,
    private val dispatch: suspend (GameAction) -> Unit,
    private val emitEffect: (GameEffect) -> Unit,
    private val config: GameFlowConfig = GameFlowConfig(),
    private val logger: Logger,
) {
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
                delay(config.dealCardDelayMs)
                dispatch(GameAction.DealCardToPlayer(i))
            }
            delay(config.dealCardDelayMs)
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

            val isCritical = BlackjackRules.isDealerCriticalDraw(state.value.dealerHand)

            if (isCritical) {
                // Emit DealerCriticalDraw BEFORE the card draw so the effect precedes PlayCardSound.
                emitEffect(GameEffect.DealerCriticalDraw)
                delay(config.dealerCriticalPreDelayMs)
            }

            dispatch(GameAction.DealerDraw)
            delay(config.dealerTurnDelayMs)
        }

        dispatch(GameAction.FinalizeGame)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun getDeck(current: GameState): List<Card> {
        val totalCards = current.rules.deckCount * BlackjackRules.CARDS_PER_DECK
        val threshold = totalCards / BlackjackRules.RESHUFFLE_THRESHOLD_DIVISOR

        // In test mode, only reshuffle when strictly empty to preserve deterministic sequences.
        // In production, reshuffle at the threshold.
        val shouldReshuffle = current.deck.isEmpty() || (current.deck.size <= threshold && !current.rules.deterministicReshuffle)

        return if (shouldReshuffle) {
            BlackjackRules.createDeck(current.rules)
        } else {
            current.deck
        }
    }

    private fun getRevealDelayMs(hand: Hand): Long {
        return if (hand.isBlackjack) config.slowRollDelayMs else config.revealDelayMs
    }
}
