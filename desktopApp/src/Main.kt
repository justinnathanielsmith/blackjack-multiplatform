package io.github.smithjustinn.blackjack

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.BalanceService
import io.github.smithjustinn.blackjack.services.JvmAudioServiceImpl
import io.github.smithjustinn.blackjack.services.NoOpHapticsService
import io.github.smithjustinn.blackjack.services.createBalanceService
import io.github.smithjustinn.blackjack.ui.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.RootContent
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.MainScope

fun main() =
    application {
        val lifecycle = LifecycleRegistry()
        val balanceService = createBalanceService()
        val root = DefaultRootComponent(DefaultComponentContext(lifecycle), balanceService)
        val appGraph =
            object : AppGraph {
                override val logger = Logger.withTag("Blackjack")
                override val audioService: AudioService = JvmAudioServiceImpl(logger)
                override val hapticsService = NoOpHapticsService
                override val balanceService: BalanceService = balanceService
                override val coroutineDispatchers = CoroutineDispatchers()
                override val applicationScope = MainScope()
            }

        Window(
            onCloseRequest = {
                appGraph.audioService.release()
                exitApplication()
            },
            title = "Blackjack"
        ) {
            RootContent(root, appGraph)
        }
    }
