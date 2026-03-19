package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.emoji_crown
import sharedui.generated.resources.result_loss
import sharedui.generated.resources.result_push
import sharedui.generated.resources.result_win
import sharedui.generated.resources.status_active
import sharedui.generated.resources.status_waiting

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
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "glowAlpha"
        )
    val glowRadiusScale by
        infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.1f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "glowRadiusScale"
        )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    alpha = glowAlpha
                    scaleX = glowRadiusScale
                    scaleY = glowRadiusScale
                }.drawWithCache {
                    val radius = size.maxDimension * 0.5f
                    val brushCenter = Offset(size.width / 2, size.height / 2)
                    val brush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = 0.4f), Color.Transparent),
                            center = brushCenter,
                            radius = radius
                        )
                    val pxRadius = cornerRadius.topStart.toPx(size, this)
                    onDrawBehind {
                        drawRoundRect(
                            brush = brush,
                            cornerRadius = CornerRadius(pxRadius),
                        )
                    }
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

    // Safe zone absorbs the badge overhangs so parent containers measure the full bounds
    val verticalSafeArea = 16.dp

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = if (isExtraCompact) 0.dp else 4.dp)
                .graphicsLayer { clip = false }
                .onGloballyPositioned { coords ->
                    if (onPositioned != null) {
                        onPositioned(
                            coords.positionInRoot() +
                                Offset(coords.size.width / 2f, coords.size.height / 2f)
                        )
                    }
                },
    ) {
        // Safe Zone: gives layout the real height including badge overhangs
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = verticalSafeArea)
        ) {
            // 1. Background layer (Glow behind if active)
            if (isActive) {
                ActiveGlowLayer(
                    cornerRadius = cornerRadius,
                    modifier = Modifier.matchParentSize(),
                )
            }

            // 2. Content box — drives the height of the background
            val contentPadding = if (isExtraCompact) 10.dp else 16.dp
            val topPadding =
                when {
                    isExtraCompact -> 20.dp
                    isCompact -> 32.dp
                    else -> 36.dp
                }
            val bottomPadding =
                when {
                    isExtraCompact -> 24.dp
                    isCompact -> 24.dp
                    else -> 28.dp
                }
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
                        .animateContentSize()
                        .defaultMinSize(minHeight = minContentHeight)
                        .padding(
                            start = contentPadding,
                            end = contentPadding,
                            top = (topPadding - verticalSafeArea).coerceAtLeast(0.dp),
                            bottom = (bottomPadding - verticalSafeArea).coerceAtLeast(0.dp),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier.graphicsLayer {
                            if (isPending) {
                                alpha = 0.8f
                            }
                        }
                ) {
                    content()
                }
            }

            // 3. Floating badges — anchored to the safe-zone box edges

            if (title != null) {
                val titleStyle =
                    if (isAnyCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
                TitleBadge(
                    title = title,
                    isActive = isActive,
                    isDealer = isDealer,
                    isCompact = isAnyCompact,
                    titleStyle = titleStyle,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 12.dp, y = (-12).dp)
                            .zIndex(1f)
                            .then(if (isAnyCompact) Modifier.scale(0.85f) else Modifier)
                )
            }

            if (showStatus && (isActive || isPending)) {
                StatusBadge(
                    isActive = isActive,
                    isCompact = isAnyCompact,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-12).dp, y = (-12).dp)
                            .zIndex(1f)
                            .then(if (isAnyCompact) Modifier.scale(0.85f) else Modifier)
                )
            }

            val badgeState =
                when {
                    isDealer -> ScoreBadgeState.DEALER
                    isActive -> ScoreBadgeState.ACTIVE
                    else -> ScoreBadgeState.WAITING
                }

            ScoreBadge(
                score = score,
                state = badgeState,
                modifier =
                    Modifier
                        .align(
                            if (badgeState ==
                                ScoreBadgeState.DEALER
                            ) {
                                Alignment.TopCenter
                            } else {
                                Alignment.BottomCenter
                            }
                        ).offset(y = if (badgeState == ScoreBadgeState.DEALER) (-18).dp else 14.dp)
                        .zIndex(2f)
                        .then(if (isAnyCompact) Modifier.scale(0.85f) else Modifier)
            )

            HandOutcomeBadge(
                result = result,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .zIndex(10f)
                        .graphicsLayer { rotationZ = -6f }
            )
        }
    }
}

@Composable
private fun BoxScope.TitleBadge(
    title: String,
    isActive: Boolean,
    isDealer: Boolean,
    isCompact: Boolean,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier,
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
            modifier
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                .background(containerColor, RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    finalBorderColor,
                    RoundedCornerShape(12.dp),
                ).padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDealer) {
            Text(
                stringResource(Res.string.emoji_crown),
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
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BoxScope.StatusBadge(
    isActive: Boolean,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
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
            modifier
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                .background(badgeColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText.uppercase(),
            color = badgeTextColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun BoxScope.HandOutcomeBadge(
    result: HandResult,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (result) {
            HandResult.WIN -> PrimaryGold
            HandResult.LOSS -> TacticalRed
            HandResult.PUSH -> Color(0xFF555555)
            HandResult.NONE -> Color.Transparent
        }

    val contentColor =
        when (result) {
            HandResult.WIN -> BackgroundDark
            else -> Color.White
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
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(tween(200)),
        exit = scaleOut(tween(150)) + fadeOut(tween(150)),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .drawWithCache {
                        val glowBrush =
                            Brush.radialGradient(
                                colors = listOf(containerColor.copy(alpha = 0.4f), Color.Transparent),
                                radius = size.maxDimension * 1.2f
                            )
                        onDrawBehind {
                            drawCircle(brush = glowBrush)
                        }
                    }
                    .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
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
                        shape = RoundedCornerShape(12.dp)
                    ).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                color = contentColor,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 4.sp,
            )
        }
    }
}
