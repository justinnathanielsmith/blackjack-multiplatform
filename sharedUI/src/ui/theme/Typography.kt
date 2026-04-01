package io.github.smithjustinn.blackjack.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Note: Using default families or system fonts but styled heavily.
// If actual custom fonts are needed later (e.g., a specific Casino Serif), they can be loaded here.

val PremiumSerif = FontFamily.Serif

val BlackjackTypography =
    Typography(
        displayLarge =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.Black,
                fontSize = 57.sp,
                letterSpacing = 2.sp
            ),
        displayMedium =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 45.sp,
                letterSpacing = 1.sp
            ),
        displaySmall =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 36.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
            ),
        headlineMedium =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = PremiumSerif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                letterSpacing = 1.2.sp
            ),
        titleMedium =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            ),
        labelLarge =
            TextStyle(
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
    )
