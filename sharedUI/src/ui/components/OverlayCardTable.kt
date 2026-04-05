package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
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
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer
import sharedui.generated.resources.emoji_crown
import sharedui.generated.resources.status_blackjack
import sharedui.generated.resources.status_bust
import sharedui.generated.resources.status_twenty_one

@Composable
fun OverlayCardTable(
    state: GameState,
    nearMissHandIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val registry = LocalDealAnimationRegistry.current
    val density = LocalDensity.current

    val coordOffsetX = registry.gameplayAreaOffset.x - registry.overlayOffset.x
    val coordOffsetY = registry.gameplayAreaOffset.y - registry.overlayOffset.y

    // shoePosition is captured in root coordinates; convert to CasinoTableLayout's
    // local space by subtracting gameplayAreaOffset (the graphicsLayer shift applied
    // to the layout). Without this, cards start ~header-height px below the shoe.
    val shoePositionLocal =
        Offset(
            registry.shoePosition.x - registry.gameplayAreaOffset.x,
            registry.shoePosition.y - registry.gameplayAreaOffset.y,
        )

    val baseCardW = with(density) { Dimensions.Card.StandardWidth.toPx() }
    val baseCardH = baseCardW / Dimensions.Card.AspectRatio

    // The actual height of the gameplay area (between header and ControlCenter),
    // measured by onGloballyPositioned in BlackjackScreen. When 0f (first frame
    // before layout), CasinoTableLayout falls back to the full overlay height.
    val gameplayAreaHeight = registry.gameplayAreaSize.height.toFloat()

    CasinoTableLayout(
        state = state,
        shoePosition = shoePositionLocal,
        gameplayAreaHeight = gameplayAreaHeight,
        onLayout = { registry.tableLayout = it },
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = coordOffsetX
                    translationY = coordOffsetY
                }
    ) {
        // 1. Active hand glow behind the cluster
        if (state.status == GameStatus.PLAYING) {
            val activeHand = state.activeHandIndex
            if (activeHand >= -1) {
                ActiveHandGlow(
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    density = density,
                    modifier = Modifier.nodeId("glow-$activeHand")
                )
            }
        }

        // 2. Positioned (landed) cards - Dealer
        val isDealerActive = state.status == GameStatus.PLAYING && state.activeHandIndex == -1
        state.dealerHand.cards.forEachIndexed { cardIndex, card ->
            val isDimmed = state.status == GameStatus.PLAYING && state.activeHandIndex != -1

            androidx.compose.runtime.key("dealer", cardIndex) {
                PositionedCardItem(
                    card = card,
                    animDelay = 0,
                    isFaceUp = card.isFaceUp,
                    isDealer = true,
                    dealerUpcard = state.dealerHand.cards.getOrNull(0),
                    dealerScore = state.dealerHand.score,
                    baseCardW = baseCardW,
                    baseCardH = baseCardH,
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    isNearMiss = false,
                    density = density,
                    isActive = isDealerActive,
                    alpha = 1f,
                    isDimmed = isDimmed,
                    cardIndexInHand = cardIndex,
                    modifier = Modifier.nodeId("dealer-card-$cardIndex")
                )
            }
        }

        // 2. Positioned (landed) cards - Player
        state.playerHands.forEachIndexed { handIndex, hand ->
            val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
            val isDimmed = state.status == GameStatus.PLAYING && handIndex != state.activeHandIndex
            val isNearMiss = handIndex == nearMissHandIndex

            hand.cards.forEachIndexed { cardIndex, card ->
                val isDoubledCard = hand.isDoubleDown && cardIndex == 2

                androidx.compose.runtime.key("player", handIndex, cardIndex) {
                    PositionedCardItem(
                        card = card,
                        animDelay = 0,
                        isFaceUp = card.isFaceUp,
                        isDealer = false,
                        dealerUpcard = null,
                        dealerScore = 0,
                        baseCardW = baseCardW,
                        baseCardH = baseCardH,
                        coordOffsetX = 0f,
                        coordOffsetY = 0f,
                        isNearMiss = isNearMiss,
                        density = density,
                        isActive = isActive,
                        alpha = 1f,
                        isDimmed = isDimmed,
                        cardIndexInHand = cardIndex,
                        isDoubleDown = isDoubledCard,
                        modifier = Modifier.nodeId("player-card-$handIndex-$cardIndex")
                    )
                }
            }
        }

        // 2.5 Positioned (landed) chips — only after cards are dealt
        if (state.status != GameStatus.BETTING) {
            state.playerHands.forEachIndexed { handIndex, hand ->
                if (hand.bet > 0) {
                    val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
                    androidx.compose.runtime.key("chip", handIndex) {
                        PositionedChipItem(
                            amount = hand.bet,
                            handIndex = handIndex,
                            handCount = state.playerHands.size,
                            coordOffsetX = 0f,
                            coordOffsetY = 0f,
                            density = density,
                            isActive = isActive,
                            modifier = Modifier.nodeId("chip-$handIndex")
                        )
                    }
                }
            }
        }

        // 3. HUD badges - Dealer
        androidx.compose.runtime.key("hud", -1) {
            HandZoneHud(
                status = state.status,
                dealerHand = state.dealerHand,
                playerHand = null,
                handResult = HandResult.NONE,
                handNetPayout = null,
                handCount = state.playerHands.size,
                coordOffsetX = 0f,
                coordOffsetY = 0f,
                density = density,
                isActive = isDealerActive,
                isDealer = true,
                handIndex = -1,
                modifier = Modifier.nodeId("hud--1")
            )
        }

        // 3. HUD badges - Players
        state.playerHands.forEachIndexed { handIndex, hand ->
            val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
            val result = state.handResult(handIndex)
            val netPayout = state.handNetPayout(handIndex)

            androidx.compose.runtime.key("hud", handIndex) {
                HandZoneHud(
                    status = state.status,
                    dealerHand = state.dealerHand,
                    playerHand = hand,
                    handResult = result,
                    handNetPayout = netPayout,
                    handCount = state.playerHands.size,
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    density = density,
                    isActive = isActive,
                    isDealer = false,
                    handIndex = handIndex,
                    modifier = Modifier.nodeId("hud-$handIndex")
                )
            }
        }
    }
}

@Composable
private fun PositionedCardItem(
    card: Card,
    animDelay: Int,
    isFaceUp: Boolean,
    isDealer: Boolean,
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
    cardIndexInHand: Int,
    modifier: Modifier = Modifier,
    isDimmed: Boolean = false,
    isDoubleDown: Boolean = false,
) {
    val progress = remember { Animatable(0f) }
    var midFlightReached by remember { mutableStateOf(false) }

    val stackBoostPx = with(density) { (cardIndexInHand * 3).dp.toPx() }
    val currentShadow = remember { Animatable(with(density) { 5.dp.toPx() } + stackBoostPx) }
    val animatedScale = remember { Animatable(1f) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        delay(animDelay.toLong())

        val zeta = 0.65f
        val durationSec = AnimationConstants.CardRevealDurationDefault / 1000f
        val stiffness =
            if (durationSec > 0) {
                val omegaN = 6.9f / (zeta * durationSec)
                (omegaN * omegaN).coerceIn(10f, 2000f)
            } else {
                Spring.StiffnessMedium
            }

        launch {
            snapshotFlow { progress.value }
                .first { it >= 0.5f }
            midFlightReached = true
        }

        launch {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = zeta, stiffness = stiffness)
            )
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }

        launch {
            val flyingElevPx = with(density) { 16.dp.toPx() }
            val landedElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
            currentShadow.animateTo(flyingElevPx, tween(AnimationConstants.CardFlightShadowRiseDuration))
            delay(AnimationConstants.CardRevealDurationDefault.toLong() + 150L)
            currentShadow.animateTo(landedElevPx, tween(AnimationConstants.CardShadowLandDuration))
        }
    }

    val targetScale =
        if (isActive && !isDealer) {
            1.1f
        } else if (isActive) {
            1.05f
        } else {
            1f
        }
    LaunchedEffect(targetScale) {
        animatedScale.animateTo(targetScale, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow))
    }
    LaunchedEffect(isActive, cardIndexInHand) {
        val targetElevPx = with(density) { if (isActive) 10.dp.toPx() else 5.dp.toPx() } + stackBoostPx
        currentShadow.animateTo(targetElevPx, tween(AnimationConstants.CardShadowLandDuration))
    }

    val wasFaceDown = remember { card.isFaceDown }

    Box(
        modifier =
            modifier
                .flightProgress(progress.asState())
                .requiredWidth(Dimensions.Card.StandardWidth)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = animatedScale.value
                    this.scaleY = animatedScale.value
                    shadowElevation = currentShadow.value
                    shape = CardShape
                    clip = false
                }
    ) {
        if (wasFaceDown) {
            DealerCard(
                card = card,
                isFaceUp = isFaceUp,
                dealerUpcard = dealerUpcard,
                dealerScore = dealerScore,
                scale = 1f,
                shadowElevation = 0.dp,
            )
        } else {
            PlayingCard(
                card = card,
                isFaceUp = isFaceUp && midFlightReached,
                scale = 1f,
                isNearMiss = isNearMiss,
                isDimmed = isDimmed,
                isDoubleDown = isDoubleDown,
                shadowElevation = 0.dp,
                spotColor = if (isActive) PrimaryGold else Color.Black
            )
        }
    }
}

@Composable
private fun PositionedChipItem(
    amount: Int,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    handIndex: Int = 0,
    handCount: Int = 1,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        val zeta = 0.65f
        val stiffness = Spring.StiffnessMedium
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = zeta, stiffness = stiffness),
        )
    }

    Box(
        modifier =
            modifier
                .flightProgress(progress.asState()),
        contentAlignment = Alignment.Center,
    ) {
        ChipStack(amount = amount, isActive = isActive)

        // Bet Label overlay — positioned at the top-right of the chips
        BetAmountBadge(
            amount = amount,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = (-16).dp.toPx()
                    }
        )
    }
}

@Composable
private fun ActiveHandGlow(
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
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

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
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
    isDealer: Boolean,
    handIndex: Int,
    modifier: Modifier = Modifier,
) {
    val isBetting = status == GameStatus.BETTING
    val showActiveIndicators = isActive && handCount > 1

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

    Box(
        modifier =
            modifier
                .fillMaxSize() // Custom Layout sets size to exactly clusterW x clusterH
                .drawBehind {
                    if (showActiveIndicators) {
                        drawRoundRect(
                            color = PrimaryGold.copy(alpha = borderGlowAlphaState.value),
                            cornerRadius = CornerRadius(12.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
    ) {
        if (showActiveIndicators) {
            ActiveHandIndicator(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer { translationY = -32.dp.toPx() }
            )
        }

        if (isDealer) {
            val displayScore =
                if (status == GameStatus.DEALER_TURN || status.isTerminal()) {
                    dealerHand.score
                } else {
                    dealerHand.visibleScore
                }

            if (!isBetting) {
                Row(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .wrapContentWidth(unbounded = true)
                            .graphicsLayer { translationY = -24.dp.toPx() },
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ScoreBadge(
                        score = displayScore,
                        state = ScoreBadgeState.DEALER,
                        label = stringResource(Res.string.dealer),
                    )
                }
            }

            HandStatusOverlay(
                hand = dealerHand,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            val hand = playerHand ?: return@Box
            val badgeState = if (isActive) ScoreBadgeState.ACTIVE else ScoreBadgeState.WAITING
            val isWinner = handResult == HandResult.WIN

            if (!isBetting) {
                val badgeAlignment =
                    when {
                        handCount == 2 && handIndex == 0 -> Alignment.BottomStart
                        handCount == 2 && handIndex == 1 -> Alignment.BottomEnd
                        handCount > 1 -> Alignment.BottomCenter
                        else -> Alignment.BottomEnd
                    }

                ScoreBadge(
                    score = hand.score,
                    state = badgeState,
                    label = null,
                    isWinner = isWinner,
                    modifier =
                        Modifier
                            .align(badgeAlignment)
                            .graphicsLayer {
                                translationX =
                                    when {
                                        handCount == 2 && handIndex == 0 -> (-12).dp.toPx()
                                        handCount == 2 && handIndex == 1 -> 12.dp.toPx()
                                        handCount > 1 -> 0f
                                        else -> 20.dp.toPx()
                                    }
                                translationY = if (handCount > 1) 28.dp.toPx() else 20.dp.toPx()
                            }
                )
            }

            HandOutcomeBadge(
                result = handResult,
                netPayout = handNetPayout,
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .graphicsLayer { rotationZ = -6f },
            )

            if (!status.isTerminal()) {
                HandStatusOverlay(
                    hand = hand,
                    modifier = Modifier.align(Alignment.Center),
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
    val visible = isBust || isBlackjack || isTwentyOne

    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(AnimationConstants.StatusMessageEnterDuration)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                ),
        exit =
            fadeOut(tween(AnimationConstants.StatusMessageExitDuration)) +
                scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        val (text, color) =
            when {
                isBust -> stringResource(Res.string.status_bust) to TacticalRed
                isBlackjack -> stringResource(Res.string.status_blackjack) to PrimaryGold
                else -> stringResource(Res.string.status_twenty_one) to PrimaryGold
            }

        Box(
            modifier =
                Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }.shadow(12.dp, RoundedCornerShape(8.dp))
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
    val bounceOffsetState =
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.ActiveHandIndicatorDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "bounceOffset",
        )

    val glowAlphaState =
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 0.8f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "indicatorGlowAlpha",
        )

    Box(
        modifier =
            modifier
                .size(24.dp, 16.dp)
                .graphicsLayer {
                    translationY = bounceOffsetState.value
                }.drawBehind {
                    // Draw a premium metallic chevron / arrow
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = glowAlphaState.value * 0.5f), Color.Transparent),
                            radius = size.maxDimension * 1.5f,
                        )
                    drawCircle(brush = glowBrush, radius = size.maxDimension * 1.5f)

                    val path =
                        androidx.compose.ui.graphics.Path().apply {
                            moveTo(0f, 0f)
                            lineTo(size.width / 2, size.height)
                            lineTo(size.width, 0f)
                            lineTo(size.width / 2, size.height * 0.4f)
                            close()
                        }

                    // Shadow/Depth
                    drawPath(
                        path = path,
                        color = Color.Black.copy(alpha = 0.4f),
                    )

                    // Main Body - Metallic Gold
                    val metallicBrush =
                        Brush.verticalGradient(
                            colors = listOf(ModernGoldLight, PrimaryGold, ModernGoldDark)
                        )
                    drawPath(
                        path = path,
                        brush = metallicBrush,
                    )

                    // Rim Highlight
                    drawPath(
                        path = path,
                        color = Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
    )
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
