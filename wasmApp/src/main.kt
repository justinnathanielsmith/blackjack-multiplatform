package io.github.smithjustinn.blackjack

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.RootContent
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.browser.document
import kotlinx.coroutines.MainScope

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val lifecycle = LifecycleRegistry()
    val root = DefaultRootComponent(DefaultComponentContext(lifecycle))
    val appGraph = object : AppGraph {
        override val logger = Logger.withTag("Blackjack")
        override val audioService: AudioService = object : AudioService {
            override fun playEffect(effect: AudioService.SoundEffect) {
                // Stub for WasmJS for now
            }
            override fun release() {}
        }
        override val coroutineDispatchers = CoroutineDispatchers()
        override val applicationScope = MainScope()
    }

    ComposeViewport(document.body!!) {
        RootContent(root, appGraph)
    }
}
