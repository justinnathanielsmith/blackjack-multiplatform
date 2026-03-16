package io.github.smithjustinn.blackjack.ui.theme

import androidx.compose.ui.unit.dp

object Dimensions {
    object ActionBar {
        val ButtonHeightNormal = 40.dp
        val ButtonHeightCompact = 32.dp
    }

    object Hand {
        val MinHeightDefault = 180.dp
        val MinHeightCompact = 120.dp
        val MinHeightExtraCompact = 100.dp
    }

    object Card {
        val StandardWidth = 140.dp
        val AspectRatio = 2.5f / 3.5f
        val OverlapOffsetRaw = -40f // Negative value for overlapping cards in dp
    }
}

object AnimationConstants {
    val CardDealDelay = 100
    val CardRevealDurationDefault = 300
    val CardRevealDurationSlow = 900
    val CardFlipDuration = 400
}
