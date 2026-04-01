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
        primary = PrimaryGold,
        onPrimary = BackgroundDark,
        secondary = PrimaryGold,
        onSecondary = BackgroundDark,
        background = BackgroundDark,
        onBackground = Color.White,
        surface = FeltGreen,
        onSurface = Color.White,
        error = TacticalRed,
        onError = Color.White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = PrimaryGold,
        onPrimary = BackgroundDark,
        secondary = PrimaryGold,
        onSecondary = BackgroundDark,
        background = FeltGreen,
        onBackground = Color.White,
        surface = FeltDark,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlackjackTypography,
        content = {
            Surface(color = Color.Transparent) {
                content()
            }
        }
    )
}
