package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.collections.immutable.toPersistentList

enum class HandStatus {
    ACTIVE,
    WAITING,
    BUSTED
}

@Composable
fun PlayerHandContainer(
    handTotal: Int,
    status: HandStatus,
    cards: List<Card>,
    bet: Int? = null,
    result: HandResult = HandResult.NONE,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val isActive = status == HandStatus.ACTIVE
    val isWaiting = status == HandStatus.WAITING
    val isBusted = status == HandStatus.BUSTED

    val alpha by animateFloatAsState(if (isWaiting) 0.6f else 1.0f)
    val saturation = if (isWaiting) 0.5f else 1.0f

    val borderColor by animateColorAsState(
        if (isActive) PrimaryGold else Color.White.copy(alpha = 0.1f)
    )

    val verticalPadding = (12 * scale).dp
    val horizontalPadding = (8 * scale).dp
    val innerPadding = (16 * scale).dp

    Box(
        modifier =
            modifier
                .alpha(alpha)
                .padding(vertical = verticalPadding, horizontal = horizontalPadding)
    ) {
        // Main Container
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
        )

        Column(
            modifier =
                Modifier
                    .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hand Row with overlapping cards
            HandRow(
                hand = Hand(cards.toPersistentList()),
                isCompact = scale < 0.9f,
                scale = scale * 0.85f
            )
        }

        // Score Bubble (Top Right)
        HandScoreBubble(
            score = handTotal,
            isActive = isActive,
            isBusted = isBusted,
            scale = scale
        )

        // Bet Chip Stack (Bottom Right)
        if (bet != null) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (8 * scale).dp, y = (8 * scale).dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
            ) {
                ChipStack(amount = bet, isActive = isActive)
            }
        }

        // Result Badge
        HandOutcomeBadge(result = result)
    }
}

@Composable
private fun BoxScope.HandScoreBubble(
    score: Int,
    isActive: Boolean,
    isBusted: Boolean,
    scale: Float = 1.0f
) {
    val backgroundColor =
        when {
            isBusted -> Color(0xFFCC2222)
            isActive -> PrimaryGold
            else -> Color(0xFF2A2A2A)
        }

    val textColor = if (isActive || isBusted) BackgroundDark else Color.White

    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = (6 * scale).dp, y = (-10 * scale).dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(backgroundColor, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = score.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}
