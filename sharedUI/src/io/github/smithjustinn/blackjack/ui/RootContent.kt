package io.github.smithjustinn.blackjack.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.di.LocalAppGraph

@Composable
fun RootContent(
    component: RootComponent,
    appGraph: AppGraph
) {
    CompositionLocalProvider(LocalAppGraph provides appGraph) {
        MaterialTheme {
            Surface {
                BlackjackContent(component.blackjackComponent)
            }
        }
    }
}
