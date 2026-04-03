package io.github.smithjustinn.blackjack.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen
import io.github.smithjustinn.blackjack.di.commonModule
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

fun BlackjackViewController(): UIViewController =
    ComposeUIViewController {
        val lifecycle = LifecycleRegistry()
        // DI wired via Koin
        val koin = getOrNull() ?: startKoin {
            modules(commonModule, iosModule)
        }.koin

        val root =
            DefaultRootComponent(
                DefaultComponentContext(lifecycle),
                balanceService = koin.get(),
                settingsRepository = koin.get(),
                audioService = koin.get(),
                hapticsService = koin.get(),
                logger = koin.get(),
            )

        RootScreen(root)
    }
