package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

enum class ScoreBadgeState {
    ACTIVE,
    DEALER,
    WAITING
}

val BadgeShape = RoundedCornerShape(12.dp)

@Composable
fun ScoreBadge(
    score: Int,
    state: ScoreBadgeState,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (state) {
        ScoreBadgeState.ACTIVE -> PrimaryGold
        ScoreBadgeState.DEALER -> Color.White
        ScoreBadgeState.WAITING -> Color(0xFF2A2A2A)
    }

    val borderColor = when (state) {
        ScoreBadgeState.ACTIVE -> Color.White.copy(alpha = 0.3f)
        ScoreBadgeState.DEALER -> Color.Transparent
        ScoreBadgeState.WAITING -> Color.White.copy(alpha = 0.3f)
    }

    val textColor = when (state) {
        ScoreBadgeState.ACTIVE -> BackgroundDark
        ScoreBadgeState.DEALER -> BackgroundDark
        ScoreBadgeState.WAITING -> Color.White
    }

    Box(
        modifier = modifier
            .background(backgroundColor, BadgeShape)
            .border(1.dp, borderColor, BadgeShape)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = score,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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
