package io.github.smithjustinn.blackjack.ui

import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import platform.UIKit.UIViewController
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.services.IosAudioServiceImpl
import io.github.smithjustinn.blackjack.services.IosHapticsServiceImpl
import io.github.smithjustinn.blackjack.services.createBalanceService
import io.github.smithjustinn.blackjack.ui.screens.RootScreen
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

fun BlackjackViewController(): UIViewController =
    ComposeUIViewController {
        val lifecycle = LifecycleRegistry()
        val appGraph =
            object : AppGraph {
                override val logger = Logger.withTag("Blackjack")
                override val audioService: AudioService = IosAudioServiceImpl(logger)
                override val hapticsService = IosHapticsServiceImpl()
                override val balanceService: BalanceService = createBalanceService()
                override val settingsRepository: SettingsRepository = createSettingsRepository()
                override val coroutineDispatchers = CoroutineDispatchers()
                override val applicationScope: CoroutineScope = MainScope()
            }
        val root =
            DefaultRootComponent(
                DefaultComponentContext(lifecycle),
                appGraph.balanceService,
                appGraph.settingsRepository
            )

        RootScreen(root, appGraph)
    }
