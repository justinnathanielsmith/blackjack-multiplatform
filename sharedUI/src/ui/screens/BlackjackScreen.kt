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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.SideBetResult
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.DebugPanel
import io.github.smithjustinn.blackjack.ui.components.GameActions
import io.github.smithjustinn.blackjack.ui.components.GameStatusMessage
import io.github.smithjustinn.blackjack.ui.components.HandContainer
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.HandRow
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.components.RulesOverlay
import io.github.smithjustinn.blackjack.ui.components.SettingsOverlay
import io.github.smithjustinn.blackjack.ui.components.StrategyGuideOverlay
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.collections.immutable.PersistentMap
import kotlinx.coroutines.delay
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

@Composable
fun BlackjackScreen(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val appSettings by component.appSettings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showStrategy by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
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

    LaunchedEffect(state.status) {
        if (state.status.isTerminal()) {
            val bet = state.currentBet
            val rules = appSettings.gameRules
            val handCount = appSettings.defaultHandCount
            delay(2000L)
            component.onAction(
                GameAction.NewGame(
                    rules = rules,
                    handCount = handCount,
                    lastBet = bet,
                )
            )
        }
    }

    LaunchedEffect(component) {
        component.effects.collect { effect: GameEffect ->
            handleGameEffect(
                effect = effect,
                hapticsService = hapticsService,
                audioService = audioService,
                isSoundMuted = appSettings.isSoundMuted
            )
        }
    }

    LaunchedEffect(state.status) {
        if (appSettings.isSoundMuted) return@LaunchedEffect
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
            val isPortrait = maxHeight > maxWidth
            val gameModifier =
                if (isPortrait) {
                    Modifier.fillMaxSize()
                } else {
                    val gameWidth = minOf(maxWidth, maxHeight * (9f / 16f))
                    val gameHeight = gameWidth * (16f / 9f)
                    Modifier.size(gameWidth, gameHeight).align(Alignment.Center)
                }

            Box(modifier = gameModifier) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing),
                ) {
                    if (appSettings.isDebugMode) {
                        DebugPanel(
                            state = state,
                            settings = appSettings,
                            onAction = component::onAction,
                            onResetBalance = component::resetBalance
                        )
                    }
                    Header(
                        balance = state.balance,
                        onSettingsClick = { showSettings = true },
                        onStrategyClick = { showStrategy = true },
                        onRulesClick = { showRules = true }
                    )

                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxSize(),
                    ) {
                        PortraitLayout(
                            state = state,
                            component = component,
                        )
                    }
                }

                // Overlay layer (full-bleed within the game bounds)
                Box(modifier = Modifier.fillMaxSize()) {
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
                        showStatus = showStatus,
                        modifier = Modifier.zIndex(5f),
                    )

                    SideBetResultOverlay(
                        results = state.sideBetResults,
                        status = state.status,
                        modifier = Modifier.zIndex(5f),
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = state.status == GameStatus.BETTING,
                        modifier = Modifier.zIndex(5f),
                        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(250)),
                        exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(200)),
                    ) {
                        BettingPhaseScreen(
                            state = state,
                            component = component,
                            audioService = audioService,
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showSettings,
                        modifier = Modifier.zIndex(10f),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
                    ) {
                        SettingsOverlay(
                            settings = appSettings,
                            onUpdateSettings = component::updateSettings,
                            onDismiss = { showSettings = false }
                        )
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = showRules,
                        modifier = Modifier.zIndex(10f),
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
                    ) {
                        RulesOverlay(
                            onDismiss = { showRules = false }
                        )
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = showStrategy,
                modifier = Modifier.zIndex(10f),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
            ) {
                StrategyGuideOverlay(
                    onDismiss = { showStrategy = false }
                )
            }
        }
    }
}

@Composable
private fun BlackjackGameOverlay(
    state: GameState,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
    showStatus: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.animation.AnimatedVisibility(
            visible = showStatus,
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            GameStatusMessage(status = state.status)
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
) {
    val hands = state.playerHands
    val isMultiHand = hands.size > 1

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(if (isMultiHand) 4.dp else 16.dp))

        val dealerDisplayScore =
            if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
        HandContainer(
            title = stringResource(Res.string.dealer),
            score = dealerDisplayScore,
            isCompact = isMultiHand,
            isExtraCompact = isMultiHand,
        ) {
            HandRow(
                state.dealerHand,
                isDealer = true,
                isCompact = isMultiHand,
                scale = if (isMultiHand) 0.82f else null,
            )
        }

        if (isMultiHand) {
            val playerCardScale = if (hands.size >= 3) 0.68f else 0.80f
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                        isCompact = true,
                        isExtraCompact = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        HandRow(hand, isCompact = true, scale = playerCardScale)
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
            HandContainer(
                title = stringResource(Res.string.you),
                score = hands[0].score,
                bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                isActive = state.status == GameStatus.PLAYING,
            ) {
                HandRow(hands[0])
            }
        }

        Spacer(modifier = Modifier.height(if (isMultiHand) 8.dp else 12.dp))

        GameActions(
            state = state,
            component = component,
            isCompact = isMultiHand,
        )
    }
}

@Composable
private fun SideBetResultOverlay(
    results: PersistentMap<SideBetType, SideBetResult>,
    status: GameStatus,
    modifier: Modifier = Modifier,
) {
    // Only show results during the playing phase (immediately after deal)
    if (results.isEmpty() || status != GameStatus.PLAYING) return

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.offset(y = 120.dp) // Below dealer hand
        ) {
            results.values.forEach { result ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { -20 }),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .border(1.dp, PrimaryGold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${result.outcomeName}: +$${result.payoutAmount}",
                            color = PrimaryGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}
