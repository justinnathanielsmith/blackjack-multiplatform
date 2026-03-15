package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.*

enum class HandResult {
    NONE,
    WIN,
    LOSS,
    PUSH
}

enum class HandStatus {
    ACTIVE,
    WAITING,
    BUSTED
}

private val ContainerShape = RoundedCornerShape(24.dp)
private val CompactContainerShape = RoundedCornerShape(12.dp)
private val ExtraCompactContainerShape = RoundedCornerShape(8.dp)
private val BadgeShape = RoundedCornerShape(12.dp)

/**
 * The unified, premium hand container for both Dealer and Player hands.
 * Features a "breaking out" score badge, status indicators, and result overlays.
 */
@Composable
fun BlackjackHandContainer(
    score: Int,
    title: String? = null,
    bet: Int? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    result: HandResult = HandResult.NONE,
    isCompact: Boolean = false,
    isExtraCompact: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val isAnyCompact = isCompact || isExtraCompact
    val borderColor =
        when {
            isActive -> PrimaryGold
            isPending -> GlassLight
            else -> Color.White.copy(alpha = 0.05f)
        }
    val backgroundColor =
        when {
            isActive -> PrimaryGold.copy(alpha = 0.1f)
            else -> GlassDark.copy(alpha = 0.3f)
        }

    val cornerRadius =
        when {
            isExtraCompact -> ExtraCompactContainerShape
            isCompact -> CompactContainerShape
            else -> ContainerShape
        }

    val horizontalPadding = if (isCompact) 8.dp else 16.dp
    val outerVerticalPadding = 6.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(horizontal = horizontalPadding, vertical = outerVerticalPadding),
    ) {
        // Visual Background + Border
        val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowAlpha"
        )
        val glowElevation by infiniteTransition.animateFloat(
            initialValue = 4f,
            targetValue = 12f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowElevation"
        )

        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .padding(vertical = 6.dp) // Offset to allow badges to overlap vertically
                    .then(
                        if (isActive) {
                            Modifier.shadow(
                                elevation = glowElevation.dp,
                                shape = cornerRadius,
                                clip = false,
                                ambientColor = PrimaryGold.copy(alpha = glowAlpha),
                                spotColor = PrimaryGold.copy(alpha = glowAlpha)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clip(cornerRadius)
                    .background(backgroundColor)
                    .border(if (isActive) 3.dp else 1.dp, borderColor, cornerRadius)
        )

        // Status Badge (Active/Waiting)
        if (isActive || isPending) {
            StatusBadge(isActive = isActive, isPending = isPending, isCompact = isAnyCompact)
        }

        // Title Badge (Dealer, Player 1, etc.)
        if (title != null) {
            val titleStyle = if (isAnyCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
            TitleBadge(title = title, isActive = isActive, isCompact = isAnyCompact, titleStyle = titleStyle)
        }

        // Score Badge: The "Breaking Out" style
        ScoreBadge(score = score, isActive = isActive, isCompact = isAnyCompact)

        val contentPadding =
            when {
                isExtraCompact -> 10.dp
                isCompact -> 16.dp
                else -> 16.dp
            }
        val topPadding =
            when {
                isExtraCompact -> 18.dp
                isCompact -> 24.dp
                else -> 24.dp
            }
        val bottomPadding = if (isExtraCompact) 8.dp else contentPadding

        val minContentHeight =
            when {
                isExtraCompact -> Dimensions.Hand.MinHeightExtraCompact
                isCompact -> Dimensions.Hand.MinHeightCompact
                else -> Dimensions.Hand.MinHeightDefault
            }

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = minContentHeight)
                    .padding(
                        start = contentPadding,
                        end = contentPadding,
                        top = topPadding,
                        bottom = bottomPadding + 6.dp
                    ),
            contentAlignment = Alignment.Center,
        ) {
            content()

            // Result Overlay (WIN/LOSS/PUSH)
            HandOutcomeBadge(result = result)

            // Bet Chip Stack
            if (bet != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp)
                            .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
                ) {
                    ChipStack(amount = bet, isActive = isActive)
                }
            }
        }
    }
}

@Composable
private fun BoxScope.TitleBadge(
    title: String,
    isActive: Boolean,
    isCompact: Boolean,
    titleStyle: TextStyle,
) {
    Box(
        modifier =
            Modifier
                .align(Alignment.TopStart)
                .offset(y = (-6).dp)
                .zIndex(1f)
                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
                .background(if (isActive) PrimaryGold else Color(0xFF2A2A2A), RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp),
                ).padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.uppercase(),
            style = titleStyle,
            color = if (isActive) BackgroundDark else Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
    }
}

@Composable
private fun BoxScope.StatusBadge(
    isActive: Boolean,
    isPending: Boolean,
    isCompact: Boolean,
) {
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
                .offset(y = (-6).dp)
                .zIndex(1f)
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
                .offset(x = 8.dp, y = (-6).dp)
                .zIndex(2f)
                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
                .background(if (isActive) PrimaryGold else Color(0xFF2A2A2A), BadgeShape)
                .border(
                    1.dp,
                    if (isActive) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    BadgeShape
                ).padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = score,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
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
internal fun BoxScope.HandOutcomeBadge(result: HandResult) {
    val color =
        when (result) {
            HandResult.WIN -> Color(0xFFFFD700) // gold
            HandResult.LOSS -> Color(0xFFCC2222) // red
            HandResult.PUSH -> Color(0xFF888888) // gray
            HandResult.NONE -> Color.Transparent
        }

    val text =
        when (result) {
            HandResult.WIN -> stringResource(Res.string.result_win)
            HandResult.LOSS -> stringResource(Res.string.result_loss)
            HandResult.PUSH -> stringResource(Res.string.result_push)
            HandResult.NONE -> ""
        }

    AnimatedVisibility(
        visible = result != HandResult.NONE,
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 600f)) + fadeIn(tween(150)),
        exit = scaleOut(tween(150)) + fadeOut(tween(150)),
        modifier = Modifier.align(Alignment.Center).zIndex(3f),
    ) {
        Box(
            modifier =
                Modifier
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
}
