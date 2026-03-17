package io.github.smithjustinn.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arkivanov.decompose.retainedComponent
import io.github.smithjustinn.blackjack.data.initDataStore
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        initDataStore(this)
        val appGraph = AndroidAppGraph(this)
        val root =
            retainedComponent {
                DefaultRootComponent(
                    it,
                    appGraph.balanceService,
                    appGraph.settingsRepository,
                    appGraph.logger
                )
            }

        setContent {
            RootScreen(root, appGraph)
        }
    }
}
