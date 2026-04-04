package io.github.smithjustinn.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import io.github.smithjustinn.blackjack.data.initDataStore
import io.github.smithjustinn.blackjack.di.commonModule
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen
import org.koin.core.context.GlobalContext.getOrNull
import org.koin.core.context.startKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initDataStore(this)
        val koin =
            getOrNull() ?: startKoin {
                modules(commonModule, androidModule(this@MainActivity.applicationContext))
            }.koin

        val root =
            retainedComponent {
                DefaultRootComponent(
                    it,
                    balanceService = koin.get(),
                    settingsRepository = koin.get(),
                    audioService = koin.get(),
                    hapticsService = koin.get(),
                    logger = koin.get()
                )
            }

        setContent {
            RootScreen(root)
        }
    }
}
