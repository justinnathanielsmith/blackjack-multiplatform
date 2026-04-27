@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.middleware.GameFlowConfig
import io.github.smithjustinn.blackjack.middleware.GameFlowMiddleware
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onClosed
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

// Abstraction boundary: presentation layer depends on this interface, not the concrete class.

/**
 * A reactive state machine that orchestrates the core logic and side effects of a Blackjack game.
 *
 * This interface serves as the primary abstraction boundary between the domain logic and the
 * presentation layer. It manages a unidirectional data flow where [GameAction]s are dispatched
 * into the machine, resulting in updates to the [state] and emissions of [effects].
 */
interface BlackjackStateMachine {
    /**
     * A [StateFlow] emitting the latest [GameState].
     *
     * UI components should observe this flow to render the current table state,
     * player/dealer hands, and available actions.
     */
    val state: StateFlow<GameState>

    /**
     * A [Flow] of [GameEffect]s triggered by state transitions.
     *
     * These represent transient events that do not affect the persistent game state,
     * such as sound effects, haptic feedback, or one-off animations.
     */
    val effects: Flow<GameEffect>

    /**
     * Dispatches a [GameAction] to the state machine for processing.
     *
     * Actions are processed sequentially and asynchronously. Invalid actions for the
     * current [GameStatus] are typically ignored or logged as warnings.
     *
     * @param action The specific action to perform (e.g., [GameAction.Hit], [GameAction.Stand]).
     */
    fun dispatch(action: GameAction)

    /**
     * Stops the state machine and closes the action channel, preventing further actions.
     *
     * Use [awaitShutdown] if you need to wait for currently buffered actions to finish processing.
     */
    fun shutdown()

    /**
     * Suspends until the state machine has completely processed its buffered actions
     * and finished its shutdown sequence.
     */
    suspend fun awaitShutdown()
}

/**
 * The core orchestration engine for the Blackjack session.
 *
 * This machine enforces a Unidirectional Data Flow (UDF) by consuming [GameAction]s
 * and emitting [GameState] updates and [GameEffect] side-effects.
 *
 * **Architecture: Dual-Channel Execution**
 * 1. **The Sync Reduction Loop**: Processes actions serially and synchronously. This loop
 *    is strictly non-suspending, ensuring the [state] always reflects the latest reduction
 *    without lag. It delegates pure business logic to the `reduce` function.
 * 2. **The Async Middleware Loop**: Processes [ReducerCommand]s emitted by the reducer.
 *    This loop handles time-sensitive transitions (e.g., deal animations, dealer turn delays)
 *    and can suspend between steps.
 *
 * **Functional Intent:**
 * - Maintain an immutable, reactively-observable session state.
 * - Decouple UI-triggered actions from the underlying physical card/timing logic.
 * - Ensure sequential processing of all game events to prevent state corruption.
 *
 * @param scope The lifetime for the internal loops. Machine is automatically [shutdown] on scope cancellation.
 * @param initialState Starting data anchor.
 */
class DefaultBlackjackStateMachine(
    private val scope: CoroutineScope,
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = BlackjackConfig.INITIAL_BALANCE),
    config: GameFlowConfig = GameFlowConfig(),
    private val logger: Logger = Logger.withTag("DefaultBlackjackStateMachine")
) : BlackjackStateMachine {
    private val _state = MutableStateFlow(initialState)

    /**
     * A [StateFlow] emitting the current [GameState].
     *
     * UI components should collect this to reactively update the display.
     */
    override val state: StateFlow<GameState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 64)
    private val isShutdown = MutableStateFlow(false)
    private val _shutdownCause = MutableStateFlow<Throwable?>(null)

    /**
     * The cause of an abnormal shutdown, or `null` for a clean shutdown via [shutdown] or scope
     * cancellation. Emits a non-null value before [effects] terminates when the action loop
     * exits due to an unhandled exception.
     */
    val shutdownCause: StateFlow<Throwable?> = _shutdownCause.asStateFlow()

    /**
     * A [Flow] of [GameEffect]s triggered by state transitions (e.g., sound effects, vibrations).
     *
     * This flow ensures that subscribers receive all effects emitted during their collection lifetime.
     * It is automatically cancelled when the state machine is shut down.
     */
    override val effects: Flow<GameEffect> =
        channelFlow {
            val collectJob = launch { _effects.collect { send(it) } }
            isShutdown.first { it }
            collectJob.cancelAndJoin()
        }

    private val actionChannel = Channel<GameAction>(Channel.UNLIMITED)
    private val commandChannel = Channel<ReducerCommand>(Channel.UNLIMITED)

    private val middleware =
        GameFlowMiddleware(
            state = _state.asStateFlow(),
            dispatch = { action ->
                // Always yield after sending so the action loop processes the action before
                // the middleware reads state back. In production this is redundant (positive
                // delays already give the loop time), but it is the correct cooperative
                // semantic and avoids any isTest flag in middleware code.
                actionChannel.send(action)
                yield()
            },
            emitEffect = ::emitEffect,
            config = config,
            logger = logger,
        )

    init {
        // 1. Pure state reduction loop — never suspends, processes actions one at a time.
        val exceptionHandler =
            CoroutineExceptionHandler { _, e ->
                if (e !is CancellationException) {
                    logger.e(e) { "SM action loop caught fatal error" }
                    _shutdownCause.value = e
                }
            }

        scope.launch(context = exceptionHandler, start = CoroutineStart.UNDISPATCHED) {
            try {
                for (action in actionChannel) {
                    logger.v { "SM reduce: $action" }
                    val result = reduce(_state.value, action)
                    _state.value = result.state
                    result.effects.forEach { emitEffect(it) }
                    result.commands.forEach { commandChannel.trySend(it) }
                    logger.v { "SM action loop finished: $action" }
                }
            } finally {
                logger.d { "SM init block finally" }
                commandChannel.close()
                isShutdown.value = true
            }
        }

        // 2. Serial middleware execution loop — suspends between commands, preventing overlap.
        scope.launch {
            for (command in commandChannel) {
                middleware.execute(command)
            }
        }
    }

    /**
     * Dispatches a [GameAction] to be processed by the state machine.
     *
     * Actions are buffered and processed sequentially. The reducer checks the current
     * [GameStatus] to ensure the action is valid for the current state.
     *
     * Actions dispatched after [shutdown] are silently ignored.
     *
     * @param action The [GameAction] to dispatch.
     */
    override fun dispatch(action: GameAction) {
        actionChannel.trySend(action).onClosed {
            logger.w { "Ignored action $action: State machine is shut down." }
        }
    }

    /**
     * Closes the action channel, preventing further actions from being dispatched.
     *
     * The internal loop continues until all currently buffered actions are processed.
     * Use [awaitShutdown] to suspend until the loop has fully drained.
     */
    override fun shutdown() {
        actionChannel.close()
    }

    /**
     * Suspends until the state machine's action loop has fully drained and shut down.
     *
     * Call after [shutdown] when you need to guarantee all buffered actions have been
     * processed before tearing down the owning scope.
     */
    override suspend fun awaitShutdown() {
        isShutdown.first { it }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private fun emitEffect(effect: GameEffect) {
        val emitted = _effects.tryEmit(effect)
        if (!emitted) logger.w { "Effect dropped (buffer full): $effect" }
    }
}
