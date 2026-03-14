package io.github.smithjustinn.blackjack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
    darkColorScheme(
        primary = ModernGold,
        onPrimary = Color.Black,
        secondary = ModernGold,
        onSecondary = Color.Black,
        background = DeepFeltGreenDark,
        onBackground = Color.White,
        surface = DeepFeltGreen,
        onSurface = Color.White,
        error = TacticalRed,
        onError = Color.White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = ModernGold,
        onPrimary = Color.Black,
        secondary = ModernGold,
        onSecondary = Color.Black,
        background = DeepFeltGreen,
        onBackground = Color.White,
        surface = DeepFeltGreenDark,
        onSurface = Color.White,
        error = TacticalRed,
        onError = Color.White,
    )

@Composable
fun BlackjackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val typography = androidx.compose.material3.Typography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = {
            Surface(color = Color.Transparent) {
                content()
            }
        }
    )
}
