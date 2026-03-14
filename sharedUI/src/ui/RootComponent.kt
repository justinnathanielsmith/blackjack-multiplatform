package io.github.smithjustinn.blackjack.ui

import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.services.BalanceService

interface RootComponent {
    val blackjackComponent: BlackjackComponent
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    balanceService: BalanceService,
) : RootComponent,
    ComponentContext by componentContext {
    override val blackjackComponent: BlackjackComponent = DefaultBlackjackComponent(this, balanceService)
}
