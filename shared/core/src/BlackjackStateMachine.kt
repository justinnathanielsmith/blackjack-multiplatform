@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.utils.secureRandom
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * The core state machine governing the game of Blackjack.
 *
 * This class serves as the single source of truth for the game state and side effects.
 * It processes [GameAction]s sequentially through a [Channel], delegating pure state
 * transitions to [reduce] and asynchronous animation timing to private middleware coroutines.
 *
 * The action loop itself **never suspends**, ensuring the UI can dispatch new actions at any
 * time — even during an in-progress deal or dealer turn animation.
 *
 * @param scope The [CoroutineScope] in which the internal action loop and middleware run.
 * @param initialState The starting [GameState]. Defaults to a new game with 1000 balance in [GameStatus.BETTING].
 * @param isTest When true, animations and delays are disabled (0ms).
 * @param logger Logger instance for internal state tracking and debugging.
 */
class BlackjackStateMachine(
    private val scope: CoroutineScope,
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = 1000),
    private val isTest: Boolean = false,
    private val logger: Logger = Logger.withTag("BlackjackStateMachine")
) {
    companion object {
        private const val DEALER_TURN_DELAY_MS = 600L
        private const val DEAL_CARD_DELAY_MS = 400L
        private const val DEALER_CRITICAL_PRE_DELAY_MS = 900L
        private const val REVEAL_DELAY_MS = 1500L
        private const val SLOW_ROLL_DELAY_MS = 2500L
    }

    private val dealerTurnDelayMs: Long get() = if (isTest) 0L else DEALER_TURN_DELAY_MS
    private val dealCardDelayMs: Long get() = if (isTest) 0L else DEAL_CARD_DELAY_MS
    private val dealerCriticalPreDelayMs: Long get() = if (isTest) 0L else DEALER_CRITICAL_PRE_DELAY_MS

    private val _state = MutableStateFlow(initialState)

    /**
     * A [StateFlow] emitting the current [GameState].
     *
     * UI components should collect this to reactively update the display.
     */
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 64)
    private val isShutdown = MutableStateFlow(false)

    /**
     * A [Flow] of [GameEffect]s triggered by state transitions (e.g., sound effects, vibrations).
     *
     * This flow ensures that subscribers receive all effects emitted during their collection lifetime.
     * It is automatically cancelled when the state machine is shut down.
     */
    val effects: Flow<GameEffect> =
        channelFlow {
            val collectJob = launch { _effects.collect { send(it) } }
            isShutdown.first { it }
            collectJob.cancelAndJoin()
        }

    private val actionChannel = Channel<GameAction>(Channel.BUFFERED)

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                for (action in actionChannel) {
                    logger.v { "SM reduce: $action" }
                    val result = reduce(_state.value, action)
                    _state.value = result.state
                    result.effects.forEach { emitEffect(it) }
                    result.commands.forEach { cmd -> scope.launch { executeCommand(cmd) } }
                    logger.v { "SM action loop finished: $action" }
                }
            } catch (e: CancellationException) {
                logger.d { "SM action loop cancelled" }
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                logger.e(e) { "SM init block caught fatal error" }
            } finally {
                logger.d { "SM init block finally" }
                isShutdown.value = true
            }
        }
    }

    /**
     * Dispatches a [GameAction] to be processed by the state machine.
     *
     * Actions are buffered and processed sequentially. The reducer checks the current
     * [GameStatus] to ensure the action is valid for the current state.
     *
     * @param action The [GameAction] to dispatch.
     */
    fun dispatch(action: GameAction) {
        actionChannel.trySend(action).getOrThrow()
    }

    /**
     * Closes the action channel, preventing further actions from being dispatched.
     *
     * The internal loop continues until all currently buffered actions are processed.
     */
    fun shutdown() {
        actionChannel.close()
    }

    // ── Middleware ────────────────────────────────────────────────────────────────

    private suspend fun executeCommand(cmd: ReducerCommand) {
        when (cmd) {
            is ReducerCommand.RunDealSequence -> executeRunDealSequence()
            is ReducerCommand.RunDealerTurn -> executeRunDealerTurn()
        }
    }

    /**
     * Middleware: orchestrates the card-by-card deal animation.
     *
     * Computes a fresh deck if needed, then dispatches [GameAction.DealCardToPlayer] and
     * [GameAction.DealCardToDealer] interleaved with [delay]s. Finishes by dispatching
     * [GameAction.ApplyInitialOutcome] after an optional reveal delay.
     */
    private suspend fun executeRunDealSequence() {
        val current = _state.value
        val freshDeck = getDeck(current)
        if (freshDeck !== current.deck) {
            dispatch(GameAction.SetDeck(freshDeck.toPersistentList()))
        }

        // Interleaved deal rounds: (P0..Pn, Dealer) × 2, second dealer card is face-down.
        for (round in 0..1) {
            val handCount = _state.value.handCount
            for (i in 0 until handCount) {
                delay(dealCardDelayMs)
                dispatch(GameAction.DealCardToPlayer(i))
            }
            delay(dealCardDelayMs)
            dispatch(GameAction.DealCardToDealer(faceDown = round == 1))
        }

        delay(getRevealDelayMs(_state.value.dealerHand))
        dispatch(GameAction.ApplyInitialOutcome)
    }

    /**
     * Middleware: orchestrates the dealer reveal and draw loop.
     *
     * Dispatches [GameAction.RevealDealerHole], waits for the reveal delay, then loops
     * drawing cards (with optional critical-draw tension) until the dealer stands.
     * Finishes by dispatching [GameAction.FinalizeGame].
     */
    private suspend fun executeRunDealerTurn() {
        dispatch(GameAction.RevealDealerHole)
        delay(getRevealDelayMs(_state.value.dealerHand))

        while (BlackjackRules.shouldDealerDraw(_state.value.dealerHand, _state.value.rules)) {
            if (_state.value.deck.isEmpty()) break // safety valve: matches original ?: break behaviour

            val hand = _state.value.dealerHand
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

        // Bolt Performance Optimization: In test mode, only reshuffle when strictly empty to preserve
        // deterministic sequences provided by tests. In production, reshuffle at the threshold.
        val shouldReshuffle = current.deck.isEmpty() || (current.deck.size <= threshold && !isTest)

        return if (shouldReshuffle) {
            BlackjackRules.createDeck(current.rules, secureRandom)
        } else {
            current.deck
        }
    }

    private fun getRevealDelayMs(hand: Hand): Long {
        if (isTest) return 0L
        val isSlowRoll =
            hand.cards.size >= 2 &&
                hand.score == 21 &&
                (hand.cards[0].rank == Rank.ACE || hand.cards[0].rank.value == 10)
        return if (isSlowRoll) SLOW_ROLL_DELAY_MS else REVEAL_DELAY_MS
    }

    private fun emitEffect(effect: GameEffect) {
        val emitted = _effects.tryEmit(effect)
        if (!emitted) logger.w { "Effect dropped (buffer full): $effect" }
    }
}
