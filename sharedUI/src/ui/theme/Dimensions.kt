package io.github.smithjustinn.blackjack.ui.theme

import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

object Dimensions {
    object Spacing {
        val Small = 8.dp
        val Medium = 16.dp
        val Large = 24.dp
    }

    object ActionBar {
        val ButtonHeightNormal = 44.dp
        val ButtonHeightCompact = 36.dp
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

val LocalShoePosition = compositionLocalOf<State<Offset?>> { mutableStateOf(null) }

/*
 * Animation API selection rule:
 *   Always-on loop while composable is present → rememberInfiniteTransition
 *   Conditional loop (runs only when flag is true) → Animatable + LaunchedEffect(flag) with while(true)
 *   State-driven value → animateXAsState / updateTransition
 *   One-shot trigger (win flash, shake, ring expand) → Animatable + LaunchedEffect(trigger), no loop
 *   Frame-by-frame physics → withFrameNanos loop
 */
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

    // Shimmer sweep (GameStatusMessage)
    val ShimmerDurationNormal = 1200
    val ShimmerDurationBlackjack = 800
    val ShimmerDelayNormal = 400
    val ShimmerDelayBlackjack = 200

    // Pulse scale (GameStatusMessage)
    val PulseDurationNormal = 600
    val PulseDurationBlackjack = 400

    // Expanding ring burst (GameStatusMessage win)
    val RingExpandDuration = 600
    val RingExpandDelay1 = 250
    val RingExpandDelay2 = 500

    // Win flash overlay (BlackjackScreen)
    val FlashInDuration = 100
    val FlashOutDurationWin = 400
    val FlashOutDurationBlackjack = 300

    // Shake offset distances (BlackjackScreen dealer win, Spring-driven)
    val ShakeDistance = 15f
    val ShakeDistanceRebound = 10f

    // Chip particle effect lifetimes (BlackjackScreen)
    val ChipEruptionLifetimeMs = 3000L
    val ChipLossLifetimeMs = 3000L
    val NearMissLifetimeMs = 1500L

    // Auto-deal and reset delays (BlackjackScreen)
    val AutoDealDelayTerminalMs = 1500L
    val ManualResetDelayMs = 2000L

    // Payout animation trigger (BlackjackScreen)
    val PayoutTriggerDelayMs = 200L

    // CasinoButton shine sweep
    val ButtonShineDuration = 2500
    val ButtonShineDelay = 500

    // Shared glow breathing (BettingSlot, GameActionButton, Header auto-deal, OverlayCardTable)
    val GlowBreatheDuration = 1200

    // Auto-deal button sonar rings (Header.kt)
    val AutoDealBorderPulseDuration = 900
    val AutoDealSonarPulseDuration = 2000
    val AutoDealSonarPulseDelay = 1000

    // Betting actions glow (BettingActions.kt)
    val BettingActionsGlowDuration = 800

    // Action bar and control panel transitions (GameActions.kt, ControlCenter.kt)
    val ActionStatusFadeDuration = 500
    val ActionPlayingSlideDuration = 300

    // Hand container expand/collapse (BlackjackHandContainer.kt)
    val HandContainerEnterDuration = 200
    val HandContainerExitDuration = 150

    // Full-panel overlay slide transitions (BlackjackScreen.kt settings/rules/strategy)
    val OverlayEnterDuration = 300
    val OverlayExitDuration = 200

    // Status message and side bets panel transitions (BlackjackScreen.kt)
    val StatusMessageEnterDuration = 200
    val StatusMessageExitDuration = 150
    val BettingPhaseEnterDuration = 250

    // Payout float animation (PayoutAnimations.kt)
    val PayoutSlideUpDuration = 1200
    val PayoutFadeDuration = 300
    val PayoutHoldDuration = 600L

    // Card slot landing micro-animations (OverlayCardTable.kt)
    val CardScaleBounceDuration = 150
    val CardFlightShadowRiseDuration = 100
    val CardShadowLandDuration = 300
    val ActiveHandIndicatorDuration = 800

    // Flying chip squash and fade (FlyingChipEffect.kt)
    val ChipSquashDuration = 50
    val ChipFadeDuration = 180
    val ChipPreFadeDelayMs = 120L

    // Confetti wave 2 (ConfettiEffect.kt, blackjack only)
    val ConfettiWave2DelayMs = 400L

    // Big Win banner (BigWinBanner.kt)
    val BigWinFlashOutDuration = 700
    val BigWinBannerLifetimeMs = 3500L

    // Dealer card reveal haptic beat (DealerCard.kt)
    val DealerRevealHapticBeatMs = 200L

    // Splash screen (SplashScreen.kt)
    val SplashScaleInDuration = 800
    val SplashFadeInDuration = 400
    val SplashDisplayDurationMs = 1200L

    // Premium Outcome Banner (GameStatusMessage.kt)
    val BannerPopDuration = 600
    val BorderRotationDuration = 3000
    val PayoutCountUpDuration = 1000
    val GlassReflectionDuration = 4000
}
