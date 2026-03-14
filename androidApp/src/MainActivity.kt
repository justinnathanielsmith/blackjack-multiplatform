package io.github.smithjustinn.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.defaultComponentContext
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.services.AndroidAudioServiceImpl
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.RootContent
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.MainScope

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = DefaultRootComponent(defaultComponentContext())
        val appGraph =
            object : AppGraph {
                override val logger = Logger.withTag("Blackjack")
                override val audioService: AudioService = AndroidAudioServiceImpl(this@MainActivity, logger)
                override val coroutineDispatchers = CoroutineDispatchers()
                override val applicationScope = MainScope()
            }

        setContent {
            RootContent(root, appGraph)
        }
    }
}
