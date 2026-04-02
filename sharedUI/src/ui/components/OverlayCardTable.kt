package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.handNetPayout
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer
import sharedui.generated.resources.emoji_crown
import sharedui.generated.resources.hand_number
import sharedui.generated.resources.status_blackjack
import sharedui.generated.resources.status_bust
import sharedui.generated.resources.status_twenty_one
import kotlin.math.roundToInt

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
            var activeZone: HandZone? = null
            for (i in 0 until tableLayout.handZones.size) {
                if (tableLayout.handZones[i].handIndex == state.activeHandIndex) {
                    activeZone = tableLayout.handZones[i]
                    break
                }
            }
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
        for (i in 0 until tableLayout.cardSlots.size) {
            val slot = tableLayout.cardSlots[i]
            val isActive = state.status == GameStatus.PLAYING && slot.handIndex == state.activeHandIndex
            val isDealer = slot.handIndex == -1
            val isDimmed = state.status == GameStatus.PLAYING && !isDealer && !isActive

            androidx.compose.runtime.key(slot.handIndex, slot.cardIndex) {
                PositionedCardItem(
                    slot = slot,
                    dealerUpcard = state.dealerHand.cards.getOrNull(0),
                    dealerScore = state.dealerHand.score,
                    baseCardW = baseCardW,
                    baseCardH = baseCardH,
                    coordOffsetX = coordOffsetX,
                    coordOffsetY = coordOffsetY,
                    isNearMiss = slot.handIndex == nearMissHandIndex,
                    density = density,
                    isActive = isActive,
                    alpha = 1f,
                    isDimmed = isDimmed,
                )
            }
        }

        // 2.5 Positioned (landed) chips — only after cards are dealt
        if (state.status != GameStatus.BETTING) {
            for (i in 0 until tableLayout.chipSlots.size) {
                val slot = tableLayout.chipSlots[i]
                val isActive = state.status == GameStatus.PLAYING && slot.handIndex == state.activeHandIndex
                androidx.compose.runtime.key(slot.handIndex) {
                    PositionedChipItem(
                        slot = slot,
                        coordOffsetX = coordOffsetX,
                        coordOffsetY = coordOffsetY,
                        density = density,
                        isActive = isActive,
                    )
                }
            }
        }

        // 3. HUD badges per zone, positioned using cluster bounds as anchor
        for (i in 0 until tableLayout.handZones.size) {
            val zone = tableLayout.handZones[i]
            val isActive = state.status == GameStatus.PLAYING && zone.handIndex == state.activeHandIndex
            val handIndex = zone.handIndex
            val playerHand = if (handIndex != -1) state.playerHands.getOrNull(handIndex) else null
            val result = if (handIndex != -1) state.handResult(handIndex) else HandResult.NONE
            val netPayout = if (handIndex != -1) state.handNetPayout(handIndex) else null

            HandZoneHud(
                zone = zone,
                status = state.status,
                dealerHand = state.dealerHand,
                playerHand = playerHand,
                handResult = result,
                handNetPayout = netPayout,
                handCount = state.playerHands.size,
                coordOffsetX = coordOffsetX,
                coordOffsetY = coordOffsetY,
                density = density,
                isActive = isActive,
            )
        }
    }
}

@Composable
private fun PositionedCardItem(
    slot: CardSlotLayout,
    dealerUpcard: Card?,
    dealerScore: Int,
    baseCardW: Float,
    baseCardH: Float,
    coordOffsetX: Float,
    coordOffsetY: Float,
    isNearMiss: Boolean,
    density: Density,
    isActive: Boolean,
    alpha: Float,
    isDimmed: Boolean = false,
) {
    val currentX = remember { Animatable(slot.startOffset.x) }
    val currentY = remember { Animatable(slot.startOffset.y) }
    val currentScale = remember { Animatable(0.5f) }
    val currentRotation = remember { Animatable(if (slot.isDealer) -45f else 45f) }
    // Shadow elevation and active-hand scale as Animatables whose values are read exclusively
    // inside the graphicsLayer lambda below — this prevents per-frame recomposition of this
    // composable while the animations are running.
    val stackBoostPx = with(density) { (slot.cardIndex * 3).dp.toPx() }
    val currentShadow = remember { Animatable(with(density) { 5.dp.toPx() } + stackBoostPx) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(slot.card, slot.centerOffset) {
        delay(slot.animDelay.toLong())

        val zeta = 0.65f
        val durationSec = slot.animDuration / 1000f
        val stiffness =
            if (durationSec > 0) {
                val omegaN = 6.9f / (zeta * durationSec)
                (omegaN * omegaN).coerceIn(10f, 2000f)
            } else {
                Spring.StiffnessMedium
            }

        launch {
            currentX.animateTo(
                targetValue = slot.centerOffset.x,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness)
            )
        }
        launch {
            currentY.animateTo(
                targetValue = slot.centerOffset.y,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness)
            )
        }
        launch {
            currentRotation.animateTo(
                targetValue = slot.rotationZ,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = stiffness)
            )
        }
        launch {
            if (currentScale.value < 1f) {
                currentScale.animateTo(1.15f, tween(AnimationConstants.CardScaleBounceDuration))
            }
            currentScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
        // Elevate the card during flight then settle to its resting elevation on landing.
        launch {
            val flyingElevPx = with(density) { 16.dp.toPx() }
            val landedElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
            currentShadow.animateTo(flyingElevPx, tween(AnimationConstants.CardFlightShadowRiseDuration))
            delay(slot.animDuration.toLong() + 150L)
            currentShadow.animateTo(landedElevPx, tween(AnimationConstants.CardShadowLandDuration))
        }
    }

    // Keep shadow and scale in sync when the active-hand state changes between deals.
    val targetScale =
        if (isActive && !slot.isDealer) {
            1.1f
        } else if (isActive) {
            1.05f
        } else {
            1f
        }
    LaunchedEffect(targetScale) {
        animatedScale.animateTo(targetScale, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(isActive, slot.cardIndex) {
        val targetElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
        currentShadow.animateTo(targetElevPx, tween(AnimationConstants.CardShadowLandDuration))
    }

    val scaledHalfW = baseCardW * slot.scale / 2f
    val scaledHalfH = baseCardH * slot.scale / 2f

    val wasFaceDown = remember(slot.card) { slot.card.isFaceDown }

    Box(
        modifier =
            Modifier
                .requiredWidth(Dimensions.Card.StandardWidth * slot.scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    translationX = currentX.value - scaledHalfW + coordOffsetX
                    translationY = currentY.value - scaledHalfH + coordOffsetY
                    rotationZ = currentRotation.value
                    this.alpha = alpha
                    this.scaleX = currentScale.value * animatedScale.value
                    this.scaleY = currentScale.value * animatedScale.value
                    // Shadow driven by Animatable — read here so only the layer re-applies
                    // each frame, not the full composable.
                    shadowElevation = currentShadow.value
                    shape = CardShape
                    clip = false
                }
    ) {
        if (wasFaceDown) {
            DealerCard(
                card = slot.card,
                isFaceUp = slot.isFaceUp,
                dealerUpcard = dealerUpcard,
                dealerScore = dealerScore,
                scale = slot.scale,
                shadowElevation = 0.dp,
            )
        } else {
            PlayingCard(
                card = slot.card,
                isFaceUp = slot.isFaceUp,
                scale = slot.scale,
                isNearMiss = isNearMiss,
                isDimmed = isDimmed,
                shadowElevation = 0.dp,
                spotColor = if (isActive) PrimaryGold else Color.Black
            )
        }
    }
}

@Composable
private fun PositionedChipItem(
    slot: ChipSlotLayout,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
) {
    val currentX = remember { Animatable(slot.startOffset.x) }
    val currentY = remember { Animatable(slot.startOffset.y) }
    val currentScale = remember { Animatable(0f) }

    LaunchedEffect(slot.amount, slot.centerOffset) {
        val zeta = 0.65f
        val stiffness = Spring.StiffnessMedium

        launch {
            currentX.animateTo(
                targetValue = slot.centerOffset.x,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness),
            )
        }
        launch {
            currentY.animateTo(
                targetValue = slot.centerOffset.y,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness),
            )
        }
        launch {
            currentScale.animateTo(1.15f, tween(AnimationConstants.CardScaleBounceDuration))
            currentScale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
        }
    }

    val chipSizeDp = 48.dp * slot.scale
    val chipHalfW = with(density) { chipSizeDp.toPx() / 2f }
    val chipHalfH = chipHalfW

    Box(
        modifier =
            Modifier
                .requiredSize(chipSizeDp)
                .graphicsLayer {
                    translationX = currentX.value - chipHalfW + coordOffsetX
                    translationY = currentY.value - chipHalfH + coordOffsetY
                    scaleX = currentScale.value
                    scaleY = currentScale.value
                },
        contentAlignment = Alignment.Center,
    ) {
        ChipStack(amount = slot.amount, isActive = isActive)
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
    // State objects stored without `by` delegate — values are read inside the graphicsLayer
    // lambda so only the layer re-applies each frame, avoiding per-frame recomposition.
    val glowAlphaState =
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 0.5f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "glowAlpha",
        )
    val glowScaleState =
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.3f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
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
                    alpha = glowAlphaState.value
                    scaleX = glowScaleState.value
                    scaleY = glowScaleState.value
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
    status: GameStatus,
    dealerHand: Hand,
    playerHand: Hand?,
    handResult: HandResult,
    handNetPayout: Int?,
    handCount: Int,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
) {
    val isDealer = zone.handIndex == -1
    val isBetting = status == GameStatus.BETTING
    // HUD elements (labels, score badges) use a gentler scale than cards so they stay readable in
    // 3-hand mode (zone.scale = 0.52 → hudScale ≈ 0.73 instead of shrinking to 52%).
    val hudScale = (zone.scale * 1.4f).coerceAtMost(1.0f)
    val clusterW = with(density) { zone.clusterSize.width.toDp() }
    val clusterH = with(density) { zone.clusterSize.height.toDp() }

    // Always create the infinite transition unconditionally to satisfy Compose composition rules.
    // The alpha value is read inside drawBehind so only the draw phase re-executes each frame,
    // not the full HandZoneHud composable.
    val borderGlowTransition = rememberInfiniteTransition(label = "borderGlowTransition")
    val borderGlowAlphaState =
        borderGlowTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "borderGlowAlpha",
        )

    // Cluster-sized box positioned over the cluster — draws active border and anchors HUD badges.
    // No graphicsLayer wrapper so child badges can overflow the cluster bounds without being clipped.
    Box(
        modifier =
            Modifier
                .requiredSize(clusterW, clusterH)
                .offset {
                    IntOffset(
                        x = (zone.clusterTopLeft.x + coordOffsetX).roundToInt(),
                        y = (zone.clusterTopLeft.y + coordOffsetY).roundToInt(),
                    )
                }.drawBehind {
                    if (isActive) {
                        drawRoundRect(
                            color = PrimaryGold.copy(alpha = borderGlowAlphaState.value),
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
                        .offset(y = (-40).dp * zone.scale)
            )
        }

        if (isDealer) {
            val displayScore =
                if (status == GameStatus.PLAYING) {
                    dealerHand.visibleScore
                } else {
                    dealerHand.score
                }

            if (!isBetting) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .wrapContentWidth(unbounded = true)
                            .offset(y = (-24).dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreBadge(
                        score = displayScore,
                        state = ScoreBadgeState.DEALER,
                        label = stringResource(Res.string.dealer),
                        modifier =
                            Modifier.graphicsLayer {
                                scaleX = hudScale
                                scaleY = hudScale
                            }
                    )
                }
            }

            HandStatusOverlay(
                hand = dealerHand,
                modifier =
                    Modifier.align(Alignment.Center).graphicsLayer {
                        scaleX = hudScale
                        scaleY = hudScale
                    },
            )
        } else {
            val handIndex = zone.handIndex
            val hand = playerHand ?: return@Box
            val result = handResult
            val netPayout = handNetPayout
            val multiHand = handCount > 1
            val badgeState = if (isActive) ScoreBadgeState.ACTIVE else ScoreBadgeState.WAITING
            val label =
                if (isActive) {
                    "YOUR TURN"
                } else if (multiHand) {
                    stringResource(
                        Res.string.hand_number,
                        handIndex + 1
                    )
                } else {
                    null
                }

            if (!isBetting) {
                ScoreBadge(
                    score = hand.score,
                    state = badgeState,
                    label = label,
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 20.dp * hudScale)
                            .graphicsLayer {
                                scaleX = hudScale
                                scaleY = hudScale
                            }
                )
            }

            HandOutcomeBadge(
                result = result,
                netPayout = netPayout,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer {
                            rotationZ = -6f
                            scaleX = hudScale
                            scaleY = hudScale
                        },
            )

            if (!status.isTerminal()) {
                HandStatusOverlay(
                    hand = hand,
                    modifier =
                        Modifier.align(Alignment.Center).graphicsLayer {
                            scaleX = hudScale
                            scaleY = hudScale
                        },
                )
            }
        }
    }
}

@Composable
private fun HandStatusOverlay(
    hand: Hand,
    modifier: Modifier = Modifier,
) {
    val isBust = hand.isBust
    val isBlackjack = hand.isBlackjack
    // Domain predicate: 3+-card 21 that is not a natural blackjack; see Hand.isTwentyOne
    val isTwentyOne = hand.isTwentyOne

    if (isBust || isBlackjack || isTwentyOne) {
        val (text, color) =
            when {
                isBust -> stringResource(Res.string.status_bust) to TacticalRed
                isBlackjack -> stringResource(Res.string.status_blackjack) to PrimaryGold
                else -> stringResource(Res.string.status_twenty_one) to PrimaryGold
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
    // No `by` delegate — value is read inside graphicsLayer to skip layout passes each frame.
    val bounceOffsetState =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 10f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.ActiveHandIndicatorDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "bounceOffset",
        )

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    translationY = bounceOffsetState.value
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
