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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
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

@Composable
private fun ActiveGlowLayer(
    cornerRadius: RoundedCornerShape,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "glowAlpha"
    )
    val glowElevation by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 20f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "glowElevation"
    )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    shadowElevation = glowElevation.dp.toPx()
                    shape = cornerRadius
                    clip = false
                    ambientShadowColor = PrimaryGold.copy(alpha = glowAlpha)
                    spotShadowColor = PrimaryGold.copy(alpha = glowAlpha)
                }.drawBehind {
                    drawRoundRect(
                        color = backgroundColor,
                        cornerRadius = CornerRadius(cornerRadius.topStart.toPx(size, this)),
                    )
                }
    )
}

@Composable
fun BlackjackHandContainer(
    score: Int,
    title: String? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    result: HandResult = HandResult.NONE,
    isCompact: Boolean = false,
    isExtraCompact: Boolean = false,
    isDealer: Boolean = false,
    showStatus: Boolean = true,
    modifier: Modifier = Modifier,
    onPositioned: ((Offset) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val isAnyCompact = isCompact || isExtraCompact
    val backgroundColor =
        when {
            isDealer -> BackgroundDark.copy(alpha = 0.9f)
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

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true)
                .padding(horizontal = horizontalPadding, vertical = 6.dp)
                .onGloballyPositioned { coords ->
                    if (onPositioned != null) {
                        onPositioned(
                            coords.positionInRoot() +
                                Offset(coords.size.width / 2f, coords.size.height / 2f)
                        )
                    }
                },
    ) {
        if (isActive) {
            ActiveGlowLayer(
                cornerRadius = cornerRadius,
                backgroundColor = backgroundColor,
                modifier = Modifier.matchParentSize().padding(vertical = 6.dp),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .padding(vertical = 6.dp)
                        .background(backgroundColor, cornerRadius)
                        .border(
                            width = if (isActive) 2.dp else 1.dp,
                            color =
                                if (isActive) {
                                    PrimaryGold.copy(
                                        alpha = 0.5f
                                    )
                                } else {
                                    Color.White.copy(alpha = if (isDealer) 0.15f else 0.08f)
                                },
                            shape = cornerRadius
                        )
            )
        }

        if (title != null) {
            val titleStyle = if (isAnyCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
            TitleBadge(
                title = title,
                isActive = isActive,
                isDealer = isDealer,
                isCompact = isAnyCompact,
                titleStyle = titleStyle
            )
        }

        if (showStatus && (isActive || isPending)) {
            StatusBadge(isActive = isActive, isPending = isPending, isCompact = isAnyCompact)
        }

        val contentPadding = if (isExtraCompact) 10.dp else 16.dp
        val topPadding =
            when {
                isExtraCompact -> 28.dp
                isCompact -> 32.dp
                else -> 36.dp
            }
        val bottomPadding =
            when {
                isExtraCompact -> 16.dp
                isCompact -> 24.dp
                else -> 28.dp
            }

        val minContentHeight =
            when {
                isExtraCompact -> Dimensions.Hand.MinHeightExtraCompact
                isCompact -> Dimensions.Hand.MinHeightCompact
                else -> Dimensions.Hand.MinHeightDefault
            }

        val badgeState =
            when {
                isDealer -> ScoreBadgeState.DEALER
                isActive -> ScoreBadgeState.ACTIVE
                else -> ScoreBadgeState.WAITING
            }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
                .defaultMinSize(minHeight = minContentHeight)
                .graphicsLayer { clip = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = contentPadding,
                        end = contentPadding,
                        top = topPadding,
                        bottom = bottomPadding
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.graphicsLayer {
                        if (isPending) {
                            alpha = 0.8f
                        }
                    }
                ) {
                    content()
                }
            }

            ScoreBadge(
                score = score,
                state = badgeState,
                modifier = Modifier
                    .align(if (badgeState == ScoreBadgeState.DEALER) Alignment.TopCenter else Alignment.BottomCenter)
                    .offset(y = if (badgeState == ScoreBadgeState.DEALER) (-4).dp else 4.dp)
                    .zIndex(2f)
                    .then(if (isAnyCompact) Modifier.scale(0.85f) else Modifier)
            )
        }

        HandOutcomeBadge(result = result)
    }
}

@Composable
private fun BoxScope.TitleBadge(
    title: String,
    isActive: Boolean,
    isDealer: Boolean,
    isCompact: Boolean,
    titleStyle: TextStyle,
) {
    val containerColor =
        when {
            isDealer -> BackgroundDark
            isActive -> PrimaryGold
            else -> Color(0xFF2A2A2A)
        }
    val contentColor =
        when {
            isDealer -> PrimaryGold
            isActive -> BackgroundDark
            else -> Color.White.copy(alpha = 0.7f)
        }
    val finalBorderColor =
        when {
            isDealer -> PrimaryGold.copy(alpha = 0.5f)
            isActive -> Color.White.copy(alpha = 0.3f)
            else -> Color.White.copy(alpha = 0.1f)
        }

    Row(
        modifier =
            Modifier
                .align(Alignment.TopStart)
                .offset(x = 12.dp, y = 8.dp)
                .zIndex(1f)
                .then(if (isCompact) Modifier.scale(0.85f) else Modifier)
                .background(containerColor, RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    finalBorderColor,
                    RoundedCornerShape(12.dp),
                ).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDealer) {
            Text(
                "👑",
                style = titleStyle.copy(fontSize = if (isCompact) 10.sp else 12.sp),
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        Text(
            text = title.uppercase(),
            style = titleStyle,
            color = contentColor,
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
                .align(Alignment.TopEnd)
                .offset(x = (-12).dp, y = 8.dp)
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
internal fun BoxScope.HandOutcomeBadge(result: HandResult) {
    val color =
        when (result) {
            HandResult.WIN -> Color(0xFFFFD700)
            HandResult.LOSS -> Color(0xFFCC2222)
            HandResult.PUSH -> Color(0xFF888888)
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
        // Shifted to the top center, slightly overlapping the top border
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = (-16).dp)
            .zIndex(3f),
    ) {
        Box(
            modifier =
                Modifier
                    .background(color, RoundedCornerShape(8.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    // Reduced padding slightly for a sleeker badge
                    .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = text.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                // Scaled down from 20.sp to fit the new top-edge placement
                fontSize = 16.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}
