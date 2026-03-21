package io.github.smithjustinn.blackjack.presentation

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository

interface RootComponent {
    val blackjackComponent: BlackjackComponent
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    balanceService: BalanceService,
    settingsRepository: SettingsRepository,
    logger: Logger,
) : RootComponent,
    ComponentContext by componentContext {
    override val blackjackComponent: BlackjackComponent =
        DefaultBlackjackComponent(this, balanceService, settingsRepository, logger)
}
