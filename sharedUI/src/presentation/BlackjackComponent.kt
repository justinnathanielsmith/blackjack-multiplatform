package io.github.smithjustinn.blackjack.presentation

import androidx.compose.runtime.Stable
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.BlackjackStateMachine
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.utils.componentScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Stable
interface BlackjackComponent {
    val state: StateFlow<GameState>
    val effects: Flow<GameEffect>
    val appSettings: StateFlow<AppSettings>

    fun onAction(action: GameAction)

    fun updateSettings(transform: (AppSettings) -> AppSettings)

    fun resetBalance()
}

class DefaultBlackjackComponent(
    componentContext: ComponentContext,
    private val balanceService: BalanceService,
    private val settingsRepository: SettingsRepository,
    private val logger: Logger,
) : BlackjackComponent,
    ComponentContext by componentContext {
    private val stateMachine = BlackjackStateMachine(componentScope, isTest = false, logger = logger)

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
                    lastBets = kotlinx.collections.immutable.persistentListOf(0)
                )
            )

            launch {
                settingsRepository.settingsFlow.collect { newSettings ->
                    val oldSettings = _appSettings.value
                    _appSettings.value = newSettings

                    if (newSettings.defaultHandCount != oldSettings.defaultHandCount) {
                        stateMachine.dispatch(GameAction.SelectHandCount(newSettings.defaultHandCount))
                    }
                    if (newSettings.gameRules != oldSettings.gameRules) {
                        stateMachine.dispatch(GameAction.UpdateRules(newSettings.gameRules))
                    }
                }
            }

            var lastSavedBalance = savedBalance
            stateMachine.state.collect { gameState ->
                if (gameState.balance != lastSavedBalance) {
                    lastSavedBalance = gameState.balance
                    balanceService.saveBalance(gameState.balance)
                }
            }
        }
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
                    lastBets = kotlinx.collections.immutable.persistentListOf(0),
                )
            )
        }
    }
}
