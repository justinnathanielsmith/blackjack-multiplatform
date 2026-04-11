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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.handNetPayout
import io.github.smithjustinn.blackjack.model.isTerminal
import io.github.smithjustinn.blackjack.ui.components.feedback.HandOutcomeBadge
import io.github.smithjustinn.blackjack.ui.components.feedback.HandResult
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadge
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadgeState
import io.github.smithjustinn.blackjack.ui.components.feedback.handResult
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer

@Composable
internal fun HandZoneHud(
    status: GameStatus,
    dealerHand: Hand,
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
    val isBetting = status == GameStatus.BETTING
    val showActiveIndicators = isActive && handCount > 1
    // Cards fully revealed only during/after dealer turn — guards domain predicates below
    val dealerFullyRevealed = status == GameStatus.DEALER_TURN || status.isTerminal()

    val borderGlowTransition = rememberInfiniteTransition(label = "borderGlowTransition")
    val borderGlowAlphaState =
        borderGlowTransition.animateFloat(
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
                .fillMaxSize() // Custom Layout sets size to exactly clusterW x clusterH
                .drawBehind {
                    if (showActiveIndicators) {
                        drawRoundRect(
                            color = PrimaryGold.copy(alpha = borderGlowAlphaState.value),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
    ) {
        if (showActiveIndicators) {
            ActiveHandIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = -32.dp.toPx() }
            )
        }

        if (isDealer) {
            if (!isBetting) {
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
                        // Domain predicates match player-hand pattern; phase-gated so
                        // hidden hole card cannot prematurely reveal bust or 21 status.
                        isBust = dealerFullyRevealed && dealerHand.isBust,
                        is21 = dealerFullyRevealed && dealerHand.isScore21,
                        state = ScoreBadgeState.DEALER,
                        label = stringResource(Res.string.dealer),
                    )
                }
            }

            HandStatusOverlay(
                hand = dealerHand,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val hand = playerHand ?: return@Box
            val badgeState = if (isActive) ScoreBadgeState.ACTIVE else ScoreBadgeState.WAITING
            val isWinner = handResult == HandResult.WIN

            if (!isBetting) {
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

            if (!status.isTerminal()) {
                HandStatusOverlay(
                    hand = hand,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}
