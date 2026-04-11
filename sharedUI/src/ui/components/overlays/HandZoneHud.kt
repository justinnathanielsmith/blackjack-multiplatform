package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.ui.components.feedback.HandOutcomeBadge
import io.github.smithjustinn.blackjack.ui.components.feedback.HandResult
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadge
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadgeState
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer

@Composable
internal fun HandZoneHud(
    // Phase-visibility flags pre-computed from GameState — no domain logic in Composables
    isBettingPhase: Boolean,
    isDealerBustVisible: Boolean,
    isDealer21Visible: Boolean,
    isRoundOver: Boolean,
    dealerDisplayScore: Int,
    playerHand: Hand?,
    handResult: HandResult,
    handNetPayout: Int?,
    handCount: Int,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
    isDealer: Boolean,
    handIndex: Int,
    modifier: Modifier = Modifier,
) {
    val showActiveIndicators = isActive && handCount > 1

    Box(
        modifier =
            modifier
                .fillMaxSize() // Custom Layout sets size to exactly clusterW x clusterH
    ) {
        if (showActiveIndicators) {
            ActiveHandDecoration(modifier = Modifier.fillMaxSize())
        }

        if (isDealer) {
            if (!isBettingPhase) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .wrapContentWidth(unbounded = true)
                            .graphicsLayer { translationY = -24.dp.toPx() },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreBadge(
                        score = dealerDisplayScore,
                        // Flags pre-computed at GameState level; hole card cannot reveal bust/21 prematurely
                        isBust = isDealerBustVisible,
                        is21 = isDealer21Visible,
                        state = ScoreBadgeState.DEALER,
                        label = stringResource(Res.string.dealer),
                    )
                }
            }

            HandStatusOverlay(
                // Dealer status only visible once hole card revealed (isDealerFullyRevealed gated upstream)
                isBust = isDealerBustVisible,
                isBlackjack = isDealer21Visible && dealerDisplayScore == 21,
                isTwentyOne = isDealer21Visible && dealerDisplayScore != 21,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val hand = playerHand ?: return@Box
            val badgeState = if (isActive) ScoreBadgeState.ACTIVE else ScoreBadgeState.WAITING
            val isWinner = handResult == HandResult.WIN

            if (!isBettingPhase) {
                val badgeAlignment =
                    when {
                        handCount == 2 && handIndex == 0 -> Alignment.BottomStart
                        handCount == 2 && handIndex == 1 -> Alignment.BottomEnd
                        handCount > 1 -> Alignment.BottomCenter
                        else -> Alignment.BottomEnd
                    }

                ScoreBadge(
                    score = hand.score,
                    isBust = hand.isBust,
                    is21 = hand.isScore21,
                    state = badgeState,
                    label = null,
                    isWinner = isWinner,
                    modifier =
                        Modifier
                            .align(badgeAlignment)
                            .graphicsLayer {
                                translationX =
                                    when {
                                        handCount == 2 && handIndex == 0 -> (-12).dp.toPx()
                                        handCount == 2 && handIndex == 1 -> 12.dp.toPx()
                                        handCount > 1 -> 0f
                                        else -> 20.dp.toPx()
                                    }
                                translationY = if (handCount > 1) 28.dp.toPx() else 20.dp.toPx()
                            }
                )
            }

            HandOutcomeBadge(
                result = handResult,
                netPayout = handNetPayout,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = -6f },
            )

            if (!isRoundOver) {
                HandStatusOverlay(
                    isBust = hand.isBust,
                    isBlackjack = hand.isBlackjack,
                    isTwentyOne = hand.isTwentyOne,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

/**
 * A private decoration composable that consolidates active-hand visual indicators
 * (border glow + chevron indicator) into a single animation loop. This is only
 * composed when the hand is active, preventing idle hands from consuming GPU.
 */
@Composable
private fun ActiveHandDecoration(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "activeHandTransition")
    val borderAlphaState =
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "borderGlowAlpha",
        )

    Box(
        modifier =
            modifier
                .drawBehind {
                    drawRoundRect(
                        color = PrimaryGold.copy(alpha = borderAlphaState.value),
                        cornerRadius = CornerRadius(12.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
    ) {
        ActiveHandIndicator(
            transition = transition,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer { translationY = -32.dp.toPx() }
        )
    }
}
