package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.BlackjackRules
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.HandOutcome
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.NeutralGray
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.result_loss
import sharedui.generated.resources.result_payout_negative
import sharedui.generated.resources.result_payout_positive
import sharedui.generated.resources.result_push
import sharedui.generated.resources.result_win

enum class HandResult {
    NONE,
    WIN,
    LOSS,
    PUSH
}

// Maps domain HandOutcome to UI HandResult — co-located with the HandResult type it returns.
internal fun GameState.handResult(index: Int): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val hand = playerHands.getOrNull(index) ?: return HandResult.NONE
    return when (BlackjackRules.determineHandOutcome(hand, dealerHand.score, dealerHand.isBust)) {
        HandOutcome.WIN, HandOutcome.NATURAL_WIN -> HandResult.WIN
        HandOutcome.PUSH -> HandResult.PUSH
        HandOutcome.LOSS -> HandResult.LOSS
    }
}

@Composable
internal fun HandOutcomeBadge(
    result: HandResult,
    modifier: Modifier = Modifier,
    netPayout: Int? = null,
) {
    val containerColor =
        when (result) {
            HandResult.WIN -> PrimaryGold
            HandResult.LOSS -> TacticalRed
            HandResult.PUSH -> NeutralGray
            HandResult.NONE -> Color.Transparent
        }
    val contentColor =
        when (result) {
            HandResult.WIN -> BackgroundDark
            else -> Color.White
        }
    val badgeText =
        when (result) {
            HandResult.WIN -> stringResource(Res.string.result_win)
            HandResult.LOSS -> stringResource(Res.string.result_loss)
            HandResult.PUSH -> stringResource(Res.string.result_push)
            HandResult.NONE -> ""
        }

    // Payout sub-label: only show when we have a non-zero net and the result is terminal.
    val payoutLabel: String? =
        when {
            netPayout == null -> null
            result == HandResult.WIN && netPayout > 0 ->
                stringResource(Res.string.result_payout_positive, netPayout)
            result == HandResult.LOSS && netPayout < 0 ->
                stringResource(Res.string.result_payout_negative, -netPayout)
            // PUSH: net is 0, no annotation needed
            else -> null
        }

    val screenReaderAnnouncement =
        if (payoutLabel != null) {
            "$badgeText. $payoutLabel"
        } else {
            badgeText
        }

    AnimatedVisibility(
        visible = result != HandResult.NONE,
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(tween(AnimationConstants.HandContainerEnterDuration)),
        exit = scaleOut(tween(AnimationConstants.HandContainerExitDuration)) + fadeOut(tween(AnimationConstants.HandContainerExitDuration)),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = screenReaderAnnouncement
                    }.drawWithCache {
                        val glowBrush =
                            Brush.radialGradient(
                                colors = listOf(containerColor.copy(alpha = 0.4f), Color.Transparent),
                                radius = size.maxDimension * 1.2f,
                            )
                        onDrawBehind { drawCircle(brush = glowBrush) }
                    }.shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                    .background(containerColor, RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color =
                            if (result ==
                                HandResult.WIN
                            ) {
                                BackgroundDark.copy(alpha = 0.3f)
                            } else {
                                Color.White.copy(alpha = 0.4f)
                            },
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 24.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = badgeText.uppercase(),
                    color = contentColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = 4.sp,
                )
                if (payoutLabel != null) {
                    Text(
                        text = payoutLabel,
                        color = contentColor.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 1.sp,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun HandOutcomeBadgePreview() {
    HandOutcomeBadge(result = HandResult.WIN, netPayout = 100)
}

@Preview
@Composable
fun HandOutcomeBadgeLossPreview() {
    HandOutcomeBadge(result = HandResult.LOSS, netPayout = -50)
}

@Preview
@Composable
fun HandOutcomeBadgePushPreview() {
    HandOutcomeBadge(result = HandResult.PUSH, netPayout = 0)
}

@Preview
@Composable
fun HandOutcomeBadgeNoPayoutPreview() {
    HandOutcomeBadge(result = HandResult.WIN)
}
