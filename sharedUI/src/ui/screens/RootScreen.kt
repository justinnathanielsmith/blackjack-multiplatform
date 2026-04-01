package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.smithjustinn.blackjack.di.AppGraph
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.RootComponent
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme

@Composable
fun RootScreen(
    component: RootComponent,
    appGraph: AppGraph,
) {
    CompositionLocalProvider(LocalAppGraph provides appGraph) {
        BlackjackTheme {
            Surface {
                Children(
                    stack = component.childStack,
                    animation = stackAnimation(fade() + scale())
                ) {
                    when (val child = it.instance) {
                        is RootComponent.Child.Splash -> SplashScreen(child.component)
                        is RootComponent.Child.Blackjack -> BlackjackScreen(child.component)
                    }
                }
            }
        }
    }
}
