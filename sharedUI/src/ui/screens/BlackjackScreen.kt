package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.GameActions
import io.github.smithjustinn.blackjack.ui.components.GameStatusMessage
import io.github.smithjustinn.blackjack.ui.components.HandContainer
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.HandRow
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer
import sharedui.generated.resources.hand_number
import sharedui.generated.resources.you

fun GameStatus.isTerminal() = this in setOf(GameStatus.PLAYER_WON, GameStatus.DEALER_WON, GameStatus.PUSH)

fun GameState.handResult(index: Int): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val hand = playerHands.getOrNull(index) ?: return HandResult.NONE
    val dealerScore = dealerHand.score
    val dealerBust = dealerHand.isBust
    return when {
        hand.isBust -> HandResult.LOSS
        dealerBust || hand.score > dealerScore -> HandResult.WIN
        hand.score == dealerScore -> HandResult.PUSH
        else -> HandResult.LOSS
    }
}

enum class LayoutMode {
    PORTRAIT,
    LANDSCAPE_COMPACT, // Phone landscape
    LANDSCAPE_WIDE, // Tablet/desktop landscape
}

@Composable
private fun androidx.compose.foundation.layout.BoxWithConstraintsScope.detectLayoutMode(): LayoutMode {
    val aspectRatio = maxWidth / maxHeight
    return when {
        maxHeight > maxWidth -> LayoutMode.PORTRAIT
        aspectRatio > 1.8f -> LayoutMode.LANDSCAPE_WIDE // Very wide (desktop/tablet)
        else -> LayoutMode.LANDSCAPE_COMPACT // Phone landscape
    }
}

@Composable
fun BlackjackScreen(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val audioService = LocalAppGraph.current.audioService
    val hapticsService = LocalAppGraph.current.hapticsService
    val shakeOffset = remember { Animatable(0f) }
    val flashAlpha = remember { Animatable(0f) }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.PLAYER_WON) {
            flashAlpha.animateTo(0.15f, tween(100))
            flashAlpha.animateTo(0f, tween(400))
        }
    }

    LaunchedEffect(component) {
        component.effects.collect { effect: GameEffect ->
            handleGameEffect(effect, hapticsService)
        }
    }

    LaunchedEffect(state.status) {
        when (state.status) {
            GameStatus.PLAYER_WON -> {
                audioService.playEffect(AudioService.SoundEffect.WIN)
            }
            GameStatus.DEALER_WON -> {
                audioService.playEffect(AudioService.SoundEffect.LOSE)
                launch {
                    shakeOffset.animateTo(15f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(-15f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(10f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(-10f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(0f, spring<Float>(stiffness = Spring.StiffnessMedium))
                }
            }
            GameStatus.PUSH -> audioService.playEffect(AudioService.SoundEffect.PUSH)
            else -> {}
        }
    }

    val backgroundBrush =
        remember(FeltGreen, FeltDark) {
            Brush.radialGradient(
                0.0f to FeltGreen,
                1.0f to FeltDark,
                radius = 2000f,
            )
        }

    BlackjackTheme {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(backgroundBrush)
                    .graphicsLayer { translationX = shakeOffset.value * density },
        ) {
            val layoutMode = detectLayoutMode()

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
            ) {
                Header(balance = state.balance)

                Box(modifier = Modifier.weight(1f)) {
                    if (state.status == GameStatus.BETTING) {
                        BettingPhaseScreen(
                            state = state,
                            component = component,
                            audioService = audioService,
                            layoutMode = layoutMode,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (layoutMode != LayoutMode.PORTRAIT) {
                        LandscapeLayout(
                            state = state,
                            component = component,
                            layoutMode = layoutMode,
                        )
                    } else {
                        PortraitLayout(
                            state = state,
                            component = component,
                            layoutMode = layoutMode,
                        )
                    }

                    // Game Status Overlay (On top of hands)
                    val showStatus =
                        state.status != GameStatus.PLAYING &&
                            state.status != GameStatus.BETTING &&
                            state.status != GameStatus.INSURANCE_OFFERED &&
                            state.status != GameStatus.IDLE

                    // Game Overlays & Status
                    BlackjackGameOverlay(
                        state = state,
                        component = component,
                        flashAlphaProvider = { flashAlpha.value },
                        layoutMode = layoutMode,
                        showStatus = showStatus,
                    )
                }
            }
        }
    }
}

@Composable
private fun BlackjackGameOverlay(
    state: GameState,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
    layoutMode: LayoutMode,
    showStatus: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showStatus,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            GameStatusMessage(status = state.status, layoutMode = layoutMode)
        }

        if (state.status == GameStatus.INSURANCE_OFFERED) {
            InsuranceOverlay(
                onInsure = { component.onAction(GameAction.TakeInsurance) },
                onDecline = { component.onAction(GameAction.DeclineInsurance) },
            )
        }

        if (state.status == GameStatus.PLAYER_WON) {
            val isBlackjack = state.playerHands.any { it.cards.size == 2 && it.score == 21 }
            ConfettiEffect(
                particleCount = if (isBlackjack) 250 else 120,
            )
        }

        val flashAlpha = flashAlphaProvider()
        if (flashAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(Color.White.copy(alpha = flashAlphaProvider()))
                        },
            )
        }
    }
}

@Composable
private fun PortraitLayout(
    state: GameState,
    component: BlackjackComponent,
    layoutMode: LayoutMode,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val dealerDisplayScore =
            if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
        HandContainer(title = stringResource(Res.string.dealer), score = dealerDisplayScore, layoutMode = layoutMode) {
            HandRow(state.dealerHand, isDealer = true, layoutMode = layoutMode)
        }

        // Anchor Box to maintain stability and provide space
        Box(
            modifier =
                Modifier
                    .weight(0.5f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Empty, space is reserved
        }

        val hands = state.playerHands
        if (hands.size > 1) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                hands.forEachIndexed { index, hand ->
                    val isActive = index == state.activeHandIndex && state.status == GameStatus.PLAYING
                    val isPending = index > state.activeHandIndex && state.status == GameStatus.PLAYING
                    HandContainer(
                        title = stringResource(Res.string.hand_number, index + 1),
                        score = hand.score,
                        bet = state.playerBets.getOrNull(index),
                        isActive = isActive,
                        isPending = isPending,
                        result = state.handResult(index),
                        layoutMode = layoutMode,
                        modifier = Modifier.weight(1f),
                    ) {
                        HandRow(hand, layoutMode = layoutMode)
                    }
                }
            }
        } else {
            HandContainer(
                title = stringResource(Res.string.you),
                score = hands[0].score,
                bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                isActive = state.status == GameStatus.PLAYING,
                layoutMode = layoutMode,
            ) {
                HandRow(hands[0], layoutMode = layoutMode)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        GameActions(
            state = state,
            component = component,
            layoutMode = layoutMode,
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: GameState,
    component: BlackjackComponent,
    layoutMode: LayoutMode,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        // Left side: Cards
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            val dealerDisplayScore =
                if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
            HandContainer(
                title = stringResource(Res.string.dealer),
                score = dealerDisplayScore,
                layoutMode = layoutMode
            ) {
                HandRow(state.dealerHand, isDealer = true, layoutMode = layoutMode)
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hands = state.playerHands
            if (hands.size > 1) {
                androidx.compose.foundation.layout.BoxWithConstraints {
                    val useVerticalSplit = maxWidth < 700.dp
                    if (useVerticalSplit) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            hands.forEachIndexed { index, hand ->
                                val isActive = index == state.activeHandIndex && state.status == GameStatus.PLAYING
                                val isPending = index > state.activeHandIndex && state.status == GameStatus.PLAYING
                                HandContainer(
                                    title = stringResource(Res.string.hand_number, index + 1),
                                    score = hand.score,
                                    bet = state.playerBets.getOrNull(index),
                                    isActive = isActive,
                                    isPending = isPending,
                                    result = state.handResult(index),
                                    layoutMode = layoutMode,
                                ) {
                                    HandRow(hand, layoutMode = layoutMode)
                                }
                            }
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            hands.forEachIndexed { index, hand ->
                                val isActive = index == state.activeHandIndex && state.status == GameStatus.PLAYING
                                val isPending = index > state.activeHandIndex && state.status == GameStatus.PLAYING
                                HandContainer(
                                    title = stringResource(Res.string.hand_number, index + 1),
                                    score = hand.score,
                                    bet = state.playerBets.getOrNull(index),
                                    isActive = isActive,
                                    isPending = isPending,
                                    result = state.handResult(index),
                                    layoutMode = layoutMode,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    HandRow(hand, layoutMode = layoutMode)
                                }
                            }
                        }
                    }
                }
            } else {
                HandContainer(
                    title = stringResource(Res.string.you),
                    score = hands[0].score,
                    bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                    isActive = state.status == GameStatus.PLAYING,
                    layoutMode = layoutMode,
                ) {
                    HandRow(hands[0], layoutMode = layoutMode)
                }
            }
        }

        // Right side: Status and Actions
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            GameActions(
                state = state,
                component = component,
                layoutMode = layoutMode,
            )
        }
    }
}
