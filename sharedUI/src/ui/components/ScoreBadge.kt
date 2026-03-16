package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

enum class ScoreBadgeState {
    ACTIVE,
    DEALER,
    WAITING
}

val BadgeShape = RoundedCornerShape(8.dp)

@Composable
fun ScoreBadge(
    score: Int,
    state: ScoreBadgeState,
    modifier: Modifier = Modifier
) {
    val backgroundColor =
        when (state) {
            ScoreBadgeState.ACTIVE -> PrimaryGold
            ScoreBadgeState.DEALER -> Color.White
            ScoreBadgeState.WAITING -> Color(0xFF2A2A2A)
        }

    val borderColor =
        when (state) {
            ScoreBadgeState.ACTIVE -> Color.White.copy(alpha = 0.3f)
            ScoreBadgeState.DEALER -> Color.Transparent
            ScoreBadgeState.WAITING -> Color.White.copy(alpha = 0.3f)
        }

    val textColor =
        when (state) {
            ScoreBadgeState.ACTIVE -> BackgroundDark
            ScoreBadgeState.DEALER -> BackgroundDark
            ScoreBadgeState.WAITING -> Color.White.copy(alpha = 0.9f)
        }

    // Spring animation for entrance/exit
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
        // Subtle pulse animation when score changes
        val pulseScale = remember { Animatable(1f) }

        LaunchedEffect(score) {
            if (score > 0) {
                pulseScale.animateTo(
                    targetValue = 1.15f,
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
        }

        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }.background(backgroundColor, BadgeShape)
                    .border(1.dp, borderColor, BadgeShape)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = textColor,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
