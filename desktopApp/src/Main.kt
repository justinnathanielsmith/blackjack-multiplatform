package io.github.smithjustinn.blackjack

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.di.commonModule
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.screens.RootScreen
import org.koin.core.context.startKoin

fun main() {
    System.setProperty("skiko.renderApi", "SOFTWARE")
    return application {
        val lifecycle = LifecycleRegistry()
        val koin =
            startKoin {
                modules(commonModule, desktopModule)
            }.koin

        val audioService = koin.get<AudioService>()

        val root =
            DefaultRootComponent(
                DefaultComponentContext(lifecycle),
                balanceService = koin.get(),
                settingsRepository = koin.get(),
                audioService = audioService,
                hapticsService = koin.get(),
                logger = koin.get(),
            )

        Window(
            onCloseRequest = {
                audioService.release()
                exitApplication()
            },
            title = "Blackjack",
            state = rememberWindowState(width = 400.dp, height = 800.dp)
        ) {
            RootScreen(root)
        }
    }
}
