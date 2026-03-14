package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.screens.LayoutMode
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.result_loss
import sharedui.generated.resources.result_push
import sharedui.generated.resources.result_win
import sharedui.generated.resources.status_active
import sharedui.generated.resources.status_waiting

@Composable
fun HandContainer(
    title: String,
    score: Int,
    bet: Int? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    result: HandResult = HandResult.NONE,
    layoutMode: LayoutMode = LayoutMode.PORTRAIT,
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

    val isCompact = layoutMode == LayoutMode.LANDSCAPE_COMPACT
    val horizontalPadding = if (isCompact) 12.dp else 16.dp
    val verticalPadding = if (isCompact) 16.dp else 24.dp
    val cornerRadius = if (isCompact) 16.dp else 24.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
    ) {
        // Visual Background
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(cornerRadius))
                    .background(backgroundColor)
                    .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(cornerRadius))
        )

        StatusBadge(isActive = isActive, isPending = isPending, isCompact = isCompact)

        ScoreBadge(score = score, isActive = isActive, isCompact = isCompact)

        val contentPadding = if (isCompact) 16.dp else 20.dp
        val topPadding = if (isCompact) 24.dp else 28.dp
        val titleStyle = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = contentPadding, end = contentPadding, top = topPadding, bottom = contentPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title.uppercase(),
                style = titleStyle,
                color = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                content()

                HandOutcomeBadge(result = result)

                if (bet != null) {
                    Box(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 12.dp, y = 12.dp)
                                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
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
    isPending: Boolean,
    isCompact: Boolean,
) {
    if (!isActive && !isPending) return

    val badgeColor = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.2f)
    val badgeTextColor = if (isActive) BackgroundDark else Color.White.copy(alpha = 0.8f)
    val badgeText =
        if (isActive) {
            stringResource(Res.string.status_active)
        } else {
            stringResource(Res.string.status_waiting)
        }

    Box(
        modifier =
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-12).dp)
                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
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
    isActive: Boolean,
    isCompact: Boolean,
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 8.dp, y = (-12).dp)
                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
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

@Composable
private fun BoxScope.HandOutcomeBadge(result: HandResult) {
    if (result == HandResult.NONE) return

    val color =
        when (result) {
            HandResult.WIN -> Color(0xFFFFD700) // gold
            HandResult.LOSS -> Color(0xFFCC2222) // red
            HandResult.PUSH -> Color(0xFF888888) // gray
            HandResult.NONE -> Color.Transparent
        }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "badgeScale",
    )

    val text =
        when (result) {
            HandResult.WIN -> stringResource(Res.string.result_win)
            HandResult.LOSS -> stringResource(Res.string.result_loss)
            HandResult.PUSH -> stringResource(Res.string.result_push)
            HandResult.NONE -> ""
        }

    Box(
        modifier =
            Modifier
                .align(Alignment.Center)
                .scale(scale)
                .background(color, RoundedCornerShape(8.dp))
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = text.uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            letterSpacing = 2.sp,
        )
    }
}
