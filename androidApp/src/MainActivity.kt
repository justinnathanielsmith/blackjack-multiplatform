package io.github.smithjustinn.blackjack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import io.github.smithjustinn.blackjack.data.initDataStore
import io.github.smithjustinn.blackjack.presentation.DefaultRootComponent
import io.github.smithjustinn.blackjack.ui.screens.RootScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initDataStore(this)
        val appGraph = AndroidAppGraph(this)
        val root = DefaultRootComponent(defaultComponentContext(), appGraph.balanceService)

        setContent {
            RootScreen(root, appGraph)
        }
    }
}
