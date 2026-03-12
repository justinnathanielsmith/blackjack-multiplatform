package io.github.smithjustinn.blackjack.ui

import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.BlackjackStateMachine
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.utils.componentScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface BlackjackComponent {
    val state: StateFlow<GameState>
    val effects: SharedFlow<GameEffect>
    fun onAction(action: GameAction)
}

class DefaultBlackjackComponent(
    componentContext: ComponentContext,
) : BlackjackComponent, ComponentContext by componentContext {
    private val stateMachine = BlackjackStateMachine(componentScope)

    override val state: StateFlow<GameState> = stateMachine.state
    override val effects: SharedFlow<GameEffect> = stateMachine.effects

    init {
        onAction(GameAction.NewGame)
    }

    override fun onAction(action: GameAction) {
        stateMachine.dispatch(action)
    }
}
