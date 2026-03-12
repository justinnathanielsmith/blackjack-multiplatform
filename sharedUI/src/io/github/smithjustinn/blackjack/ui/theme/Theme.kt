package io.github.smithjustinn.blackjack.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ModernGold,
    onPrimary = Color.Black,
    secondary = OakMedium,
    onSecondary = ModernGold,
    background = FeltGreenDark,
    onBackground = Color.White,
    surface = DarkOak,
    onSurface = ModernGold,
)

private val LightColorScheme = lightColorScheme(
    primary = ModernGold,
    onPrimary = Color.Black,
    secondary = OakMedium,
    onSecondary = ModernGold,
    background = FeltGreenLight,
    onBackground = Color.White,
    surface = OakMedium,
    onSurface = ModernGold,
)

@Composable
fun BlackjackTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = {
            Surface(color = Color.Transparent) {
                content()
            }
        }
    )
}
