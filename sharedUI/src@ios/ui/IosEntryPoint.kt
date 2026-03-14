package io.github.smithjustinn.blackjack.ui

import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.services.IosAudioServiceImpl
import io.github.smithjustinn.blackjack.services.IosHapticsServiceImpl
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.MainScope
import platform.UIKit.UIViewController

fun BlackjackViewController(): UIViewController =
    ComposeUIViewController {
        val lifecycle = LifecycleRegistry()
        val root = DefaultRootComponent(DefaultComponentContext(lifecycle))
        val appGraph =
            object : AppGraph {
                override val logger = Logger.withTag("Blackjack")
                override val audioService: AudioService = IosAudioServiceImpl(logger)
                override val hapticsService = IosHapticsServiceImpl()
                override val coroutineDispatchers = CoroutineDispatchers()
                override val applicationScope = MainScope()
            }

        RootContent(root, appGraph)
    }
