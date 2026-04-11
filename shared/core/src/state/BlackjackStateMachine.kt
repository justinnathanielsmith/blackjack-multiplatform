@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack.state
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
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
interface BlackjackStateMachine {
    val state: StateFlow<GameState>
    val effects: Flow<GameEffect>

    fun dispatch(action: GameAction)

    fun shutdown()

    suspend fun awaitShutdown()
}

/**
 * The core state machine governing the game of Blackjack.
 *
 * This class serves as the single source of truth for the game state and side effects.
 * It processes [GameAction]s sequentially through a [Channel], delegating pure state
 * transitions to [reduce] and asynchronous animation timing to [GameFlowMiddleware].
 *
 * The action loop itself **never suspends**, ensuring the UI can dispatch new actions at any
 * time — even during an in-progress deal or dealer turn animation. Middleware commands are
 * processed **serially** via a dedicated command channel, preventing animation interleaving.
 *
 * @param scope The [CoroutineScope] in which the internal loops and middleware run.
 * @param initialState The starting [GameState]. Defaults to a new game with 1000 balance in [GameStatus.BETTING].
 * @param isTest When true, animations and delays are disabled (0ms).
 * @param logger Logger instance for internal state tracking and debugging.
 */
class DefaultBlackjackStateMachine(
    private val scope: CoroutineScope,
    initialState: GameState = GameState(status = GameStatus.BETTING, balance = BlackjackConfig.INITIAL_BALANCE),
    private val isTest: Boolean = false,
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
            isTest = isTest,
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
