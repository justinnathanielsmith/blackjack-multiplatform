package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.score_accessibility_blackjack
import sharedui.generated.resources.score_accessibility_bust
import sharedui.generated.resources.score_accessibility_dealer
import sharedui.generated.resources.score_accessibility_generic

enum class ScoreBadgeState {
    ACTIVE,
    DEALER,
    WAITING
}

// Sleek pill shape
val BadgeShape = RoundedCornerShape(percent = 50)

@Composable
fun ScoreBadge(
    score: Int,
    state: ScoreBadgeState,
    modifier: Modifier = Modifier
) {
    val isBust = score > 21
    val is21 = score == 21

    val backgroundColor =
        when {
            isBust -> TacticalRed
            is21 -> PrimaryGold
            state == ScoreBadgeState.ACTIVE -> PrimaryGold
            state == ScoreBadgeState.DEALER -> BackgroundDark
            else -> Color(0xFF1A1A1A) // Deep dark for waiting
        }

    val borderColor =
        when {
            isBust -> Color.White.copy(alpha = 0.6f)
            is21 || state == ScoreBadgeState.ACTIVE -> Color.White.copy(alpha = 0.5f)
            state == ScoreBadgeState.DEALER -> PrimaryGold
            else -> Color.White.copy(alpha = 0.15f)
        }

    val textColor =
        when {
            isBust -> Color.White
            is21 || state == ScoreBadgeState.ACTIVE -> BackgroundDark
            state == ScoreBadgeState.DEALER -> PrimaryGold
            else -> Color.White.copy(alpha = 0.7f)
        }

    val announcement =
        when {
            isBust -> stringResource(Res.string.score_accessibility_bust, score)
            is21 -> stringResource(Res.string.score_accessibility_blackjack)
            state == ScoreBadgeState.DEALER -> stringResource(Res.string.score_accessibility_dealer, score)
            else -> stringResource(Res.string.score_accessibility_generic, score)
        }

    AnimatedVisibility(
        visible = score > 0,
        enter =
            scaleIn(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
            ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        val pulseScale = remember { Animatable(1f) }

        LaunchedEffect(score) {
            pulseScale.animateTo(
                targetValue = 1.2f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    )
            )
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
            )
        }

        Box(
            modifier =
                Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announcement
                    }.graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }.shadow(
                        elevation =
                            if (state == ScoreBadgeState.ACTIVE ||
                                state == ScoreBadgeState.DEALER ||
                                is21 ||
                                isBust
                            ) {
                                12.dp
                            } else {
                                4.dp
                            },
                        shape = BadgeShape,
                        spotColor = if (state == ScoreBadgeState.ACTIVE) PrimaryGold else backgroundColor
                    ).then(
                        if (state == ScoreBadgeState.ACTIVE) {
                            Modifier.border(2.dp, PrimaryGold.copy(alpha = 0.8f), BadgeShape)
                        } else {
                            Modifier
                        }
                    ).background(backgroundColor, BadgeShape)
                    .border(1.5.dp, borderColor, BadgeShape)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = score,
                transitionSpec = {
                    val springSpec =
                        spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    (scaleIn(animationSpec = springSpec) + fadeIn())
                        .togetherWith(scaleOut() + fadeOut())
                },
                label = "scoreRoll"
            ) { targetScore ->
                Text(
                    text = targetScore.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ScoreBadgePreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 18, state = ScoreBadgeState.ACTIVE)
        }
    }
}

@Preview
@Composable
private fun ScoreBadgeBustPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 23, state = ScoreBadgeState.ACTIVE)
        }
    }
}

@Preview
@Composable
private fun ScoreBadgeTwentyOnePreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 21, state = ScoreBadgeState.ACTIVE)
        }
    }
}
