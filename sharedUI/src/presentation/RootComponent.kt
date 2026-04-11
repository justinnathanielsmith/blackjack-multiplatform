package io.github.smithjustinn.blackjack.presentation

import co.touchlab.kermit.Logger
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceCurrent
import com.arkivanov.decompose.value.Value
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.infra.componentScope
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.HapticsService
import io.github.smithjustinn.blackjack.state.DefaultBlackjackStateMachine
import kotlinx.serialization.Serializable

interface RootComponent {
    val childStack: Value<ChildStack<Config, Child>>

    sealed class Child {
        class Splash(
            val component: SplashComponent
        ) : Child()

        class Blackjack(
            val component: BlackjackComponent
        ) : Child()
    }

    @Serializable
    sealed class Config {
        @Serializable
        data object Splash : Config()

        @Serializable
        data object Blackjack : Config()
    }
}

class DefaultRootComponent(
    componentContext: ComponentContext,
    private val balanceService: BalanceService,
    private val settingsRepository: SettingsRepository,
    private val audioService: AudioService,
    private val hapticsService: HapticsService,
    private val logger: Logger,
) : RootComponent,
    ComponentContext by componentContext {
    private val navigation = StackNavigation<RootComponent.Config>()

    override val childStack: Value<ChildStack<RootComponent.Config, RootComponent.Child>> =
        childStack(
            source = navigation,
            serializer = RootComponent.Config.serializer(),
            initialConfiguration = RootComponent.Config.Splash,
            handleBackButton = true,
            childFactory = ::createChild
        )

    private fun createChild(
        config: RootComponent.Config,
        componentContext: ComponentContext
    ): RootComponent.Child {
        return when (config) {
            is RootComponent.Config.Splash ->
                RootComponent.Child.Splash(
                    DefaultSplashComponent(
                        componentContext = componentContext,
                        onFinished = {
                            navigation.replaceCurrent(RootComponent.Config.Blackjack)
                        }
                    )
                )
            is RootComponent.Config.Blackjack ->
                RootComponent.Child.Blackjack(
                    DefaultBlackjackComponent(
                        componentContext = componentContext,
                        balanceService = balanceService,
                        settingsRepository = settingsRepository,
                        audioService = audioService,
                        hapticsService = hapticsService,
                        logger = logger,
                        stateMachine = DefaultBlackjackStateMachine(componentContext.componentScope, logger = logger),
                    )
                )
        }
    }
}
