package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_active

@Composable
fun HandContainer(
    title: String,
    score: Int,
    bet: Int? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val borderColor =
        if (isActive) {
            PrimaryGold
        } else if (isPending) {
            GlassLight
        } else {
            Color.White.copy(alpha = 0.05f)
        }
    val backgroundColor =
        if (isActive) {
            PrimaryGold.copy(alpha = 0.1f)
        } else if (isPending) {
            Color.Black.copy(alpha = 0.2f)
        } else {
            GlassDark.copy(alpha = 0.3f)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
    ) {
        // Visual Background
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(backgroundColor)
                    .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(24.dp))
        )

        StatusBadge(isActive = isActive, isPending = isPending)

        ScoreBadge(score = score, isActive = isActive)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                content()

                if (bet != null) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 12.dp, y = 12.dp)
                    ) {
                        BetChip(amount = bet, isActive = isActive)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.StatusBadge(
    isActive: Boolean,
    isPending: Boolean
) {
    if (!isActive && !isPending) return

    val badgeColor = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.2f)
    val badgeTextColor = if (isActive) BackgroundDark else Color.White.copy(alpha = 0.8f)
    val badgeText = if (isActive) stringResource(Res.string.status_active) else "WAITING"

    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .background(badgeColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text = badgeText.uppercase(),
            color = badgeTextColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
        )
    }
}

@Composable
private fun BoxScope.ScoreBadge(
    score: Int,
    isActive: Boolean
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-12).dp)
                .background(if (isActive) PrimaryGold else Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                ).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.AnimatedContent(
            targetState = score,
            transitionSpec = {
                androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.fadeOut()
            },
            label = "scoreRoll"
        ) { targetScore ->
            Text(
                text = targetScore.toString(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isActive) BackgroundDark else Color.White,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
