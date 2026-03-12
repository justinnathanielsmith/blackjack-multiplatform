package io.github.smithjustinn.blackjack.ui

import com.arkivanov.decompose.ComponentContext

interface RootComponent {
    val blackjackComponent: BlackjackComponent
}

class DefaultRootComponent(
    componentContext: ComponentContext,
) : RootComponent,
    ComponentContext by componentContext {
    override val blackjackComponent: BlackjackComponent = DefaultBlackjackComponent(this)
}
