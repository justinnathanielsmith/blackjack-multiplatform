package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.RootComponent

@Composable
fun RootScreen(
    component: RootComponent,
    appGraph: AppGraph,
) {
    CompositionLocalProvider(LocalAppGraph provides appGraph) {
        MaterialTheme {
            Surface {
                BlackjackScreen(component.blackjackComponent)
            }
        }
    }
}
