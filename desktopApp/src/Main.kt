package io.github.smithjustinn.blackjack

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen

fun main() =
    application {
        val lifecycle = LifecycleRegistry()
        val appGraph = DesktopAppGraph()
        val root = DefaultRootComponent(DefaultComponentContext(lifecycle), appGraph.balanceService)

        Window(
            onCloseRequest = {
                appGraph.audioService.release()
                exitApplication()
            },
            title = "Blackjack",
        ) {
            RootScreen(root, appGraph)
        }
    }
