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

@Stable
interface BlackjackComponent {
    val state: StateFlow<GameState>
    val effects: Flow<GameEffect>
    val appSettings: StateFlow<AppSettings>

    // audioService is exposed for the animation orchestrator (presentation layer)
    // and must NOT be called directly from @Composable UI — use onPlay*() methods instead.
    val audioService: AudioService
    val hapticsService: HapticsService

    // Covers all tap-feedback sounds (buttons, chips, seats) — plays CLICK effect.
    fun onPlayClick()

    // Composite reset: clears main bet and side bets together — these always reset as a pair.
    fun onResetBets()

    fun onPlayDeal()

    fun onPlayPlink(amount: Int)

    fun onAction(action: GameAction)

    fun updateSettings(transform: (AppSettings) -> AppSettings)

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
