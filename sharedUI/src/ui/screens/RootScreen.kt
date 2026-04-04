package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import io.github.smithjustinn.blackjack.presentation.RootComponent
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme

@Composable
fun RootScreen(component: RootComponent,) {
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
