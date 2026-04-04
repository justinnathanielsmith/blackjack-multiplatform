package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.BlackjackConfig
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.totalNetPayout
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationOrchestrator
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationState
import io.github.smithjustinn.blackjack.ui.animation.PayoutInstance
import io.github.smithjustinn.blackjack.ui.components.BetChip
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.components.ControlCenter
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.OverlayCardTable
import io.github.smithjustinn.blackjack.ui.components.handResult
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.DragAndDropContainer
import io.github.smithjustinn.blackjack.utils.LocalDragAndDropState
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun BlackjackScreen(
    component: BlackjackComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()
    val appSettings by component.appSettings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showStrategy by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val animState = remember { BlackjackAnimationState() }
    var headerBalanceOffset by remember { mutableStateOf(Offset.Zero) }

    var selectedAmount by remember { mutableStateOf(BlackjackConfig.DEFAULT_CHIP_AMOUNT) }
    val onResetBet =
        remember(component) {
            {
                component.onPlayClick()
                component.onAction(GameAction.ResetBet)
                component.onAction(GameAction.ResetSideBets)
            }
        }
    val onDeal =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Deal)
            }
        }
    val onChipSelected =
        remember(component) {
            { amount: Int ->
                selectedAmount = amount
                component.onPlayPlink(amount)
            }
        }

    val isTerminal by remember { derivedStateOf { state.status.isTerminal() } }
    val isMultiHand by remember { derivedStateOf { state.playerHands.size > 1 } }

    val onAutoDealToggle =
        remember(component) {
            { component.updateSettings { it.copy(isAutoDealEnabled = !it.isAutoDealEnabled) } }
        }
    val onSettingsClick = remember { { showSettings = true } }
    val onStrategyClick = remember { { showStrategy = true } }
    val onRulesClick = remember { { showRules = true } }
    val onDismissSettings = remember { { showSettings = false } }
    val onDismissRules = remember { { showRules = false } }
    val onDismissStrategy = remember { { showStrategy = false } }

    val dealRegistry = remember { DealAnimationRegistry() }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.BETTING) {
            dealRegistry.tableLayout = null
        }
    }

    // handZones[0] = dealer, handZones[1..N] = player hands
    // Bolt Performance Optimization: Remove delegated 'by' and read .value in draw scope to avoid screen-wide recomposition during animation.
    val activeHandHighlightPositionState =
        animateOffsetAsState(
            targetValue =
                if (state.status == GameStatus.PLAYING) {
                    val zone = dealRegistry.tableLayout?.handZones?.getOrNull(state.activeHandIndex + 1)
                    if (zone != null) zone.clusterCenter + dealRegistry.gameplayAreaOffset else Offset.Zero
                } else {
                    Offset.Zero
                },
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "activeHandHighlight",
        )

    // Trigger payout animations when results are calculated and it's a win
    LaunchedEffect(state.status, state.playerHands) {
        if (state.status.isTerminal()) {
            for (index in 0 until state.playerHands.size) {
                val result = state.handResult(index)
                if (result == HandResult.WIN) {
                    val bet = state.playerHands.getOrNull(index)?.bet ?: 0
                    if (bet > 0) {
                        delay(AnimationConstants.PayoutTriggerDelayMs)
                        val zone = dealRegistry.tableLayout?.handZones?.getOrNull(index + 1)
                        val target =
                            if (zone != null) {
                                zone.clusterCenter + dealRegistry.gameplayAreaOffset
                            } else {
                                Offset.Zero
                            }
                        animState.activePayouts.add(PayoutInstance(Random.nextLong(), bet, target))
                    }
                }
            }
        }
    }

    // Animation orchestration: effects pipeline + state-driven flash/shake
    LaunchedEffect(component) {
        BlackjackAnimationOrchestrator.orchestrate(
            effects = component.effects,
            stateFlow = component.state,
            animState = animState,
            audioService = component.audioService,
            hapticsService = component.hapticsService,
        )
    }

    BlackjackTheme {
        DragAndDropContainer(modifier = modifier) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                TableSurface()

                // Enforce a portrait-like aspect ratio (9:16) if the window is too wide (letterboxing)
                val gameModifier =
                    if (maxHeight > maxWidth) {
                        Modifier.fillMaxSize()
                    } else {
                        val gameWidth = maxHeight * (9f / 16f)
                        Modifier.size(gameWidth, maxHeight).align(Alignment.Center)
                    }

                Box(
                    modifier =
                        gameModifier
                            .drawBehind {
                                // 5. Active Hand Highlight — read here so only this draw scope re-runs each frame.
                                val highlightPos = activeHandHighlightPositionState.value
                                if (highlightPos != Offset.Zero) {
                                    val highlightRadius = size.maxDimension * 0.35f
                                    // Inner intense spot
                                    drawCircle(
                                        brush =
                                            Brush.radialGradient(
                                                colors = listOf(PrimaryGold.copy(alpha = 0.15f), Color.Transparent),
                                                center = highlightPos,
                                                radius = highlightRadius * 0.4f
                                            ),
                                        center = highlightPos,
                                        radius = highlightRadius * 0.4f
                                    )
                                    // Outer soft throw
                                    drawCircle(
                                        brush =
                                            Brush.radialGradient(
                                                colors = listOf(PrimaryGold.copy(alpha = 0.08f), Color.Transparent),
                                                center = highlightPos,
                                                radius = highlightRadius
                                            ),
                                        center = highlightPos,
                                        radius = highlightRadius
                                    )
                                }
                            }.graphicsLayer {
                                val densityVal = density
                                translationX = animState.shakeOffset.value * densityVal
                            },
                ) {
                    CompositionLocalProvider(LocalDealAnimationRegistry provides dealRegistry) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(safeDrawingInsets().only(WindowInsetsSides.Horizontal)),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            headerBalanceOffset = it.positionInRoot() + Offset(80.dp.value, 40.dp.value)
                                        }
                            ) {
                                Header(
                                    balance = state.balance,
                                    isAutoDealEnabled = appSettings.isAutoDealEnabled,
                                    onAutoDealToggle = onAutoDealToggle,
                                    onSettingsClick = onSettingsClick,
                                    onStrategyClick = onStrategyClick,
                                    onRulesClick = onRulesClick
                                )
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .onGloballyPositioned { coords ->
                                            dealRegistry.gameplayAreaOffset = coords.positionInRoot()
                                            dealRegistry.gameplayAreaSize = coords.size
                                        },
                            ) {
                                BlackjackLayout(
                                    state = state,
                                    component = component,
                                    dealRegistry = dealRegistry,
                                )
                            }

                            ControlCenter(
                                state = state,
                                component = component,
                                selectedAmount = selectedAmount,
                                onChipSelected = onChipSelected,
                                onResetBet = onResetBet,
                                onDeal = onDeal,
                                isCompact = isMultiHand,
                            )
                        }

                        // Overlay layer (full-bleed within the game bounds)
                        Box(
                            modifier =
                                Modifier.fillMaxSize().onGloballyPositioned { coords ->
                                    dealRegistry.overlayOffset = coords.positionInRoot()
                                }
                        ) {
                            // Cards + HUD rendered in overlay (below other overlays)
                            OverlayCardTable(
                                state = state,
                                nearMissHandIndex = animState.nearMissHandIndex,
                                modifier = Modifier.zIndex(1f),
                            )

                            // Game Overlays & Status
                            GameOverlay(
                                sideBetResults = state.sideBetResults,
                                status = state.status,
                                playerHands = state.playerHands,
                                netPayout = state.totalNetPayout(),
                                component = component,
                                flashAlphaProvider = { animState.flashAlpha.value },
                                flashColorProvider = { animState.flashColor },
                                isPaused = { animState.isPaused },
                            )

                            BettingLayer(
                                status = state.status,
                                handCount = state.handCount,
                                sideBets = state.sideBets,
                                playerHands = state.playerHands,
                                animState = animState,
                                component = component,
                                audioService = component.audioService,
                                selectedAmount = selectedAmount,
                            )

                            OverlayLayer(
                                showSettings = showSettings,
                                showRules = showRules,
                                showStrategy = showStrategy,
                                appSettings = appSettings,
                                component = component,
                                onDismissSettings = onDismissSettings,
                                onDismissRules = onDismissRules,
                                onDismissStrategy = onDismissStrategy,
                            )
                        }
                    } // CompositionLocalProvider
                }
            } // End of outer BoxWithConstraints

            val dragAndDropState = LocalDragAndDropState.current
            if (dragAndDropState.isDragging && dragAndDropState.dragItem is Int) {
                val amount = dragAndDropState.dragItem as Int
                BetChip(
                    amount = amount,
                    chipColor = ChipUtils.chipColor(amount),
                    textColor = ChipUtils.chipTextColor(amount),
                    isActive = true, // Larger shadow and lift effect
                    modifier =
                        Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                translationX = dragAndDropState.dragPosition.x - (56.dp.toPx() / 2f)
                                translationY = dragAndDropState.dragPosition.y - (56.dp.toPx() / 2f)
                                scaleX = 1.15f
                                scaleY = 1.15f
                                alpha = 0.95f
                            }.zIndex(100f)
                )
            }
        } // End of DragAndDropContainer
    } // End of BlackjackTheme
}

@Composable
private fun BlackjackLayout(
    state: GameState,
    component: BlackjackComponent,
    dealRegistry: DealAnimationRegistry,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Shoe Position Anchor in top-right corner
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(80.dp, 110.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        dealRegistry.shoePosition =
                            Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f,
                            )
                    }
        )
    }
}
