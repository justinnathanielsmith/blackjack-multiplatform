package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.BlackjackRules
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.HandOutcome
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.PositionedCardEntry
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer
import sharedui.generated.resources.emoji_crown
import sharedui.generated.resources.hand_number
import sharedui.generated.resources.status_blackjack
import sharedui.generated.resources.status_bust
import kotlin.math.roundToInt

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
fun OverlayCardTable(
    state: GameState,
    nearMissHandIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val registry = LocalDealAnimationRegistry.current
    val density = LocalDensity.current
    val tableLayout = registry.tableLayout ?: return

    val coordOffsetX = registry.gameplayAreaOffset.x - registry.overlayOffset.x
    val coordOffsetY = registry.gameplayAreaOffset.y - registry.overlayOffset.y

    val baseCardW = with(density) { Dimensions.Card.StandardWidth.toPx() }
    val baseCardH = baseCardW / Dimensions.Card.AspectRatio

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Active hand glow behind the cluster
        if (state.status == GameStatus.PLAYING) {
            val activeZone = tableLayout.handZones.getOrNull(state.activeHandIndex + 1)
            if (activeZone != null) {
                ActiveHandGlow(
                    zone = activeZone,
                    coordOffsetX = coordOffsetX,
                    coordOffsetY = coordOffsetY,
                    density = density,
                )
            }
        }

        // 2. Positioned (landed) cards
        registry.positionedCards.forEach { entry ->
            val isActive = state.status == GameStatus.PLAYING && entry.handIndex == state.activeHandIndex
            val isDealer = entry.handIndex == -1
            val alpha = if (state.status == GameStatus.PLAYING && !isDealer && !isActive) 0.7f else 1f
            val elevationScale = if (isActive) 1.05f else 1f

            PositionedCardItem(
                entry = entry,
                state = state,
                baseCardW = baseCardW,
                baseCardH = baseCardH,
                coordOffsetX = coordOffsetX,
                coordOffsetY = coordOffsetY,
                isNearMiss = entry.handIndex == nearMissHandIndex,
                density = density,
                isActive = isActive,
                alpha = alpha,
                elevationScale = elevationScale,
            )
        }

        // 3. HUD badges per zone, positioned using cluster bounds as anchor
        tableLayout.handZones.forEach { zone ->
            val isActive = state.status == GameStatus.PLAYING && zone.handIndex == state.activeHandIndex
            val isDealer = zone.handIndex == -1
            val alpha = if (state.status == GameStatus.PLAYING && !isDealer && !isActive) 0.7f else 1f
            val scale = if (isActive) 1.05f else 1f

            HandZoneHud(
                zone = zone,
                state = state,
                coordOffsetX = coordOffsetX,
                coordOffsetY = coordOffsetY,
                density = density,
                isActive = isActive,
                alpha = alpha,
                scale = scale,
            )
        }
    }
}

@Composable
private fun PositionedCardItem(
    entry: PositionedCardEntry,
    state: GameState,
    baseCardW: Float,
    baseCardH: Float,
    coordOffsetX: Float,
    coordOffsetY: Float,
    isNearMiss: Boolean,
    density: Density,
    isActive: Boolean,
    alpha: Float,
    elevationScale: Float,
) {
    val registry = LocalDealAnimationRegistry.current
    val isFlying = registry.isFlying(entry.handIndex, entry.cardIndex)

    val animatedOffset by animateOffsetAsState(
        targetValue = entry.targetOffset,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cardPos",
    )

    // Dynamic shadow based on flying state, active state, and card stack position
    val stackBoost = (entry.cardIndex * 2).dp
    val shadowElevation by androidx.compose.animation.core.animateDpAsState(
        targetValue =
            if (isFlying) {
                16.dp
            } else if (isActive) {
                (8.dp + stackBoost)
            } else {
                (2.dp + stackBoost)
            },
        animationSpec = tween(300),
        label = "shadowElevation"
    )

    val scaledHalfW = baseCardW * entry.scale * elevationScale / 2f
    val scaledHalfH = baseCardH * entry.scale * elevationScale / 2f

    Box(
        modifier =
            Modifier
                .requiredWidth(Dimensions.Card.StandardWidth * entry.scale * elevationScale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    translationX = animatedOffset.x - scaledHalfW + coordOffsetX
                    translationY = animatedOffset.y - scaledHalfH + coordOffsetY
                    rotationZ = entry.rotationZ
                    this.alpha = alpha
                    this.scaleX = elevationScale
                    this.scaleY = elevationScale
                }
    ) {
        if (entry.isHoleCard) {
            DealerCard(
                card = entry.card,
                isFaceUp = entry.isFaceUp,
                dealerUpcard = state.dealerHand.cards.getOrNull(0),
                dealerScore = state.dealerHand.score,
                scale = entry.scale * elevationScale,
                shadowElevation = shadowElevation,
            )
        } else {
            PlayingCard(
                card = entry.card,
                isFaceUp = entry.isFaceUp,
                scale = entry.scale * elevationScale,
                isNearMiss = isNearMiss,
                shadowElevation = shadowElevation,
                spotColor = if (isActive) PrimaryGold else Color.Black
            )
        }
    }
}

@Composable
private fun ActiveHandGlow(
    zone: HandZone,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowAlpha",
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "glowScale",
    )

    val glowW = zone.clusterSize.width * 1.6f
    val glowH = zone.clusterSize.height * 1.6f

    Box(
        modifier =
            Modifier
                .requiredSize(
                    width = with(density) { glowW.toDp() },
                    height = with(density) { glowH.toDp() },
                ).graphicsLayer {
                    translationX = zone.clusterCenter.x - glowW / 2f + coordOffsetX
                    translationY = zone.clusterCenter.y - glowH / 2f + coordOffsetY
                    alpha = glowAlpha
                    scaleX = glowScale
                    scaleY = glowScale
                }.drawWithCache {
                    val radius = size.maxDimension * 0.5f
                    val center = Offset(size.width / 2, size.height / 2)
                    val brush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = radius,
                        )
                    onDrawBehind {
                        drawCircle(brush = brush, center = center, radius = radius)
                    }
                }
    )
}

@Composable
private fun HandZoneHud(
    zone: HandZone,
    state: GameState,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
    alpha: Float,
    scale: Float,
) {
    val isDealer = zone.handIndex == -1
    val clusterW = with(density) { zone.clusterSize.width.toDp() }
    val clusterH = with(density) { zone.clusterSize.height.toDp() }

    val borderTransition = rememberInfiniteTransition(label = "borderGlowTransition")
    val borderGlowAlpha by borderTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "borderGlowAlpha",
    )

    // Transparent cluster-sized box positioned over the cluster — used for badge anchoring
    Box(
        modifier =
            Modifier
                .requiredSize(clusterW * scale, clusterH * scale)
                .offset {
                    IntOffset(
                        x =
                            ((zone.clusterTopLeft.x + coordOffsetX) - (clusterW.toPx() * (scale - 1f) / 2f))
                                .roundToInt(),
                        y =
                            ((zone.clusterTopLeft.y + coordOffsetY) - (clusterH.toPx() * (scale - 1f) / 2f))
                                .roundToInt(),
                    )
                }.graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scale
                    this.scaleY = scale
                }.drawBehind {
                    if (isActive) {
                        drawRoundRect(
                            color = PrimaryGold.copy(alpha = borderGlowAlpha),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
    ) {
        if (isActive) {
            ActiveHandIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-40).dp)
            )
        }

        if (isDealer) {
            val displayScore =
                if (state.status == GameStatus.PLAYING) {
                    state.dealerHand.visibleScore
                } else {
                    state.dealerHand.score
                }

            Row(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-18).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HudTitleBadge(
                    title = stringResource(Res.string.dealer),
                    isDealer = true,
                    isActive = false,
                )
                ScoreBadge(
                    score = displayScore,
                    state = ScoreBadgeState.DEALER,
                )
            }

            HandStatusOverlay(
                hand = state.dealerHand,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val handIndex = zone.handIndex
            val hand = state.playerHands.getOrNull(handIndex) ?: return@Box
            val result = state.handResult(handIndex)
            val multiHand = state.playerHands.size > 1
            val badgeState = if (isActive) ScoreBadgeState.ACTIVE else ScoreBadgeState.WAITING
            val bet = state.playerBets.getOrNull(handIndex) ?: 0

            if (multiHand) {
                HudTitleBadge(
                    title = stringResource(Res.string.hand_number, handIndex + 1),
                    isDealer = false,
                    isActive = isActive,
                    modifier =
                        Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-8).dp, y = (-16).dp),
                )
            }

            // Tightly clustered ScoreBadge + Bet (as a Chip)
            Row(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (bet > 0) {
                    Box(modifier = Modifier.requiredSize(44.dp), contentAlignment = Alignment.Center) {
                        BetChip(
                            amount = bet,
                            isActive = isActive,
                            modifier =
                                Modifier.graphicsLayer {
                                    scaleX = 0.75f
                                    scaleY = 0.75f
                                }
                        )
                    }
                }
                ScoreBadge(
                    score = hand.score,
                    state = badgeState,
                )
            }

            HandOutcomeBadge(
                result = result,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = -6f },
            )

            HandStatusOverlay(
                hand = hand,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun HandStatusOverlay(
    hand: Hand,
    modifier: Modifier = Modifier,
) {
    val isBust = hand.isBust
    val isBlackjack = hand.score == 21 && hand.cards.size == 2

    if (isBust || isBlackjack) {
        val (text, color) =
            if (isBust) {
                stringResource(Res.string.status_bust) to TacticalRed
            } else {
                stringResource(Res.string.status_blackjack) to PrimaryGold
            }

        Box(
            modifier =
                modifier
                    .shadow(12.dp, RoundedCornerShape(8.dp))
                    .background(color, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = if (isBust) Color.White else BackgroundDark,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun ActiveHandIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "indicatorTransition")
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "bounceOffset",
    )

    Box(
        modifier =
            modifier
                .offset(y = bounceOffset.dp)
                .graphicsLayer {
                    shadowElevation = 8.dp.toPx()
                    shape = RoundedCornerShape(4.dp)
                    clip = true
                }.background(PrimaryGold)
                .padding(4.dp)
    ) {
        Text(
            text = "▼", // Simple chevron
            color = BackgroundDark,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
internal fun HudTitleBadge(
    title: String,
    isDealer: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when {
            isDealer -> BackgroundDark.copy(alpha = 0.85f)
            isActive -> PrimaryGold.copy(alpha = 0.95f)
            else -> Color(0xCC2A2A2A)
        }
    val contentColor =
        when {
            isDealer -> PrimaryGold
            isActive -> BackgroundDark
            else -> Color.White.copy(alpha = 0.9f)
        }
    val borderColor =
        when {
            isDealer -> PrimaryGold.copy(alpha = 0.4f)
            isActive -> Color.White.copy(alpha = 0.5f)
            else -> Color.White.copy(alpha = 0.15f)
        }

    Row(
        modifier =
            modifier
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                .background(containerColor, RoundedCornerShape(12.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isDealer) {
            Text(
                text = stringResource(Res.string.emoji_crown),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
    }
}
