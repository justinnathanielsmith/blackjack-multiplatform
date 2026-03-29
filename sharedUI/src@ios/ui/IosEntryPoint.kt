package io.github.smithjustinn.blackjack.ui

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen
import platform.UIKit.UIViewController

fun BlackjackViewController(): UIViewController =
    ComposeUIViewController {
        val lifecycle = LifecycleRegistry()
        // DI wired via dedicated IosAppGraph, consistent with AndroidAppGraph / DesktopAppGraph
        val appGraph = IosAppGraph()
        val root =
            DefaultRootComponent(
                DefaultComponentContext(lifecycle),
                appGraph.balanceService,
                appGraph.settingsRepository,
                appGraph.logger,
            )

        RootScreen(root, appGraph)
    }
