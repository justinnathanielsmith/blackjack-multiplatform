package io.github.smithjustinn.blackjack.presentation

import androidx.compose.runtime.Stable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.action.GameEffect
import io.github.smithjustinn.blackjack.domain.BalanceService
import io.github.smithjustinn.blackjack.domain.SettingsRepository
import io.github.smithjustinn.blackjack.infra.componentScope
import io.github.smithjustinn.blackjack.model.AppSettings
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.isTerminal
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.state.BlackjackStateMachine
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

// Presentation-layer timing for auto-deal state machine loop; kept here to avoid
// the presentation layer depending on the UI theme package.
private const val AUTO_DEAL_DELAY_TERMINAL_MS = 1500L
private const val MANUAL_RESET_DELAY_MS = 2000L

/**
 * The **Interaction Bridge** between the UI layer (Compose) and the domain engine.
 *
 * This Decompose component orchestrates the lifetime of a single Blackjack session.
 * It transforms raw UI gestures into [GameAction]s and propagates the resulting
 * [state] and [effects] back to the view.
 *
 * **Functional Intent:**
 * - **Lifecycle Management**: Owns the [componentScope] which governs the
 *   persistence of balance, settings collection, and the [BlackjackStateMachine].
 * - **Action Bridging**: Provides stable callbacks (e.g., [onAction], [onResetBets])
 *   that decoupling the UI from the internal action dispatch logic.
 * - **State Projection**: Collects and maps domain state (from the state machine)
 *   into a reactive stream suitable for Composable consumption.
 *
 * Constraints: This is the primary entry point for all presentation-layer logic.
 */
@Stable
interface BlackjackComponent {
    /**
     * A [StateFlow] emitting the current [GameState].
     *
     * This is the single source of truth for all visual elements on the table,
     * including hands, bets, and game phase.
     */
    val state: StateFlow<GameState>

    /**
     * A [Flow] of [GameEffect]s triggered by the state machine.
     *
     * These represent transient events like sound triggers, vibrations, or one-off
     * animations (e.g., chip eruptions) that are not part of the persistent state.
     */
    val effects: Flow<GameEffect>

    /**
     * A [StateFlow] for user-persisted [AppSettings].
     *
     * Controls global behaviors like auto-deal, sound muting, and house rule variations.
     */
    val appSettings: StateFlow<AppSettings>

    /**
     * The underlying [AudioService] used for game sounds.
     *
     * @see onPlayClick
     * @see onPlayDeal
     * @see onPlayPlink
     * @warning This service should be consumed primarily by animation orchestrators in the
     * presentation layer. **Do not call directly from @Composable functions**; use the
     * provided `onPlay*` callbacks instead to ensure predictable side-effect timing.
     */
    val audioService: AudioService

    /**
     * The underlying [HapticsService] for device feedback.
     *
     * Same usage constraints as [audioService] apply.
     */
    val hapticsService: HapticsService

    /** Plays a standard UI tap/click sound. */
    fun onPlayClick()

    /**
     * Resets both the main bet and all active side bets on the table.
     *
     * This composite action ensures that the player can clear their entire table stake
     * with a single interaction during the betting phase.
     */
    fun onResetBets()

    /** Plays the dealer's card-flipping/dealing sound. */
    fun onPlayDeal()

    /** Plays a chip-related interaction sound. */
    fun onPlayPlink(amount: Int)

    /**
     * Dispatches a [GameAction] to the state machine.
     *
     * @param action The specific player or engine action to process.
     */
    fun onAction(action: GameAction)

    /**
     * Updates the persistent user settings via a transformation function.
     *
     * @param transform Lambda receiving current settings and returning the updated version.
     */
    fun updateSettings(transform: (AppSettings) -> AppSettings)

    /**
     * Resets the player's bankroll to the default starting balance and begins a new game.
     *
     * Used typically when the player reaches a $0 balance and needs a "re-buy".
     */
    fun resetBalance()
}

class DefaultBlackjackComponent(
    componentContext: ComponentContext,
    private val balanceService: BalanceService,
    private val settingsRepository: SettingsRepository,
    override val audioService: AudioService,
    override val hapticsService: HapticsService,
    private val logger: Logger,
    private val stateMachine: BlackjackStateMachine,
) : BlackjackComponent,
    ComponentContext by componentContext {
    override val state: StateFlow<GameState> = stateMachine.state
    override val effects: Flow<GameEffect> = stateMachine.effects

    private val _appSettings = MutableStateFlow(AppSettings())
    override val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    init {
        componentScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            _appSettings.value = settings
            val savedBalance = balanceService.balanceFlow.first()
            stateMachine.dispatch(
                GameAction.NewGame(
                    savedBalance,
                    rules = settings.gameRules,
                    handCount = settings.defaultHandCount,
                    previousBets = persistentListOf(0)
                )
            )

            launch {
                settingsRepository.settingsFlow.collect { newSettings ->
                    val oldSettings = _appSettings.value
                    _appSettings.value = newSettings
                    audioService.isMuted = newSettings.isSoundMuted

                    if (newSettings.defaultHandCount != oldSettings.defaultHandCount) {
                        stateMachine.dispatch(GameAction.SelectHandCount(newSettings.defaultHandCount))
                    }
                    if (newSettings.gameRules != oldSettings.gameRules) {
                        stateMachine.dispatch(GameAction.UpdateRules(newSettings.gameRules))
                    }
                }
            }

            launch {
                var lastSavedBalance = savedBalance
                stateMachine.state.collect { gameState ->
                    if (gameState.balance != lastSavedBalance) {
                        lastSavedBalance = gameState.balance
                        balanceService.saveBalance(gameState.balance)
                    }
                }
            }

            launch {
                stateMachine.state
                    .map { it.status.isTerminal() }
                    .distinctUntilChanged()
                    .collectLatest { isTerminal ->
                        if (isTerminal) {
                            val currentState = stateMachine.state.value
                            val autoDealEnabled = _appSettings.value.isAutoDealEnabled
                            val previousBets = currentState.lastBets
                            val handCount = currentState.handCount
                            val lastSideBets = currentState.lastSideBets

                            delay(
                                if (autoDealEnabled) {
                                    AUTO_DEAL_DELAY_TERMINAL_MS
                                } else {
                                    MANUAL_RESET_DELAY_MS
                                }
                            )

                            stateMachine.dispatch(
                                GameAction.NewGame(
                                    rules = _appSettings.value.gameRules,
                                    handCount = handCount,
                                    previousBets = previousBets,
                                    lastSideBets = lastSideBets,
                                )
                            )

                            if (autoDealEnabled) {
                                val postResetState = stateMachine.state.value
                                if (postResetState.playerHands.all { it.bet > 0 }) {
                                    stateMachine.dispatch(GameAction.Deal)
                                } else {
                                    updateSettings { it.copy(isAutoDealEnabled = false) }
                                }
                            }
                        }
                    }
            }
        }
    }

    override fun onPlayClick() {
        audioService.playEffect(AudioService.SoundEffect.CLICK)
    }

    override fun onResetBets() {
        audioService.playEffect(AudioService.SoundEffect.CLICK)
        stateMachine.dispatch(GameAction.ResetBet)
        stateMachine.dispatch(GameAction.ResetSideBets)
    }

    override fun onPlayDeal() {
        audioService.playEffect(AudioService.SoundEffect.DEAL)
    }

    override fun onPlayPlink(amount: Int) {
        audioService.playEffect(AudioService.SoundEffect.PLINK)
    }

    override fun onAction(action: GameAction) {
        stateMachine.dispatch(action)
    }

    override fun updateSettings(transform: (AppSettings) -> AppSettings) {
        componentScope.launch {
            settingsRepository.update(transform)
        }
    }

    override fun resetBalance() {
        componentScope.launch {
            balanceService.resetBalance()
            stateMachine.dispatch(
                GameAction.NewGame(
                    initialBalance = BalanceService.DEFAULT_BALANCE,
                    rules = _appSettings.value.gameRules,
                    handCount = _appSettings.value.defaultHandCount,
                    previousBets = persistentListOf(0),
                )
            )
        }
    }
}
