package io.github.smithjustinn.blackjack.presentation

import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.BlackjackStateMachine
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.utils.componentScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

interface BlackjackComponent {
    val state: StateFlow<GameState>
    val effects: SharedFlow<GameEffect>

    fun onAction(action: GameAction)
}

class DefaultBlackjackComponent(
    componentContext: ComponentContext,
    private val balanceService: BalanceService,
) : BlackjackComponent,
    ComponentContext by componentContext {
    private val stateMachine = BlackjackStateMachine(componentScope)

    override val state: StateFlow<GameState> = stateMachine.state
    override val effects: SharedFlow<GameEffect> = stateMachine.effects

    init {
        componentScope.launch {
            val savedBalance = balanceService.balanceFlow.first()
            stateMachine.dispatch(GameAction.NewGame(savedBalance))

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
}
