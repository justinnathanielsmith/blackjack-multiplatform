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
        val SmallCardThreshold = 65.dp

        // Font scale multipliers (fraction of card width)
        val CourtCrownScale = 0.3f
        val CourtRankScale = 0.5f
        val AcePipScale = 0.55f
        val NumberRankScaleNormal = 0.55f
        val NumberRankScaleTen = 0.45f
        val NumberPipScale = 0.25f
    }
}

object AnimationConstants {
    val CardDealDelay = 100
    val CardRevealDurationDefault = 300
    val CardRevealDurationSlow = 900
    val CardFlipDuration = 400
    val NearMissInDuration = 300
    val NearMissHoldDuration = 600L
    val NearMissOutDuration = 600
    val CardDealOffsetDealer = -300f
    val CardDealOffsetPlayer = 300f
}
