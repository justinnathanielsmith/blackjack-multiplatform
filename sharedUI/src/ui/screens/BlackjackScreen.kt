package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction

import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationOrchestrator
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationState
import io.github.smithjustinn.blackjack.ui.animation.ChipEruptionInstance
import io.github.smithjustinn.blackjack.ui.animation.ChipLossInstance
import io.github.smithjustinn.blackjack.ui.animation.PayoutInstance
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.isStatusVisible
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.totalNetPayout
import io.github.smithjustinn.blackjack.ui.components.BetChip
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.components.ControlCenter
import io.github.smithjustinn.blackjack.ui.components.GameStatusMessage
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.components.OverlayCardTable
import io.github.smithjustinn.blackjack.ui.components.RulesOverlay
import io.github.smithjustinn.blackjack.ui.components.SettingsOverlay
import io.github.smithjustinn.blackjack.ui.components.Shoe
import io.github.smithjustinn.blackjack.ui.components.StrategyGuideOverlay
import io.github.smithjustinn.blackjack.ui.components.computeTableLayout
import io.github.smithjustinn.blackjack.ui.components.handResult
import io.github.smithjustinn.blackjack.ui.effects.ChipEruptionEffect
import io.github.smithjustinn.blackjack.ui.effects.ChipLossEffect
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.PayoutEffect
import io.github.smithjustinn.blackjack.ui.effects.SparkleEffect
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDeepEdge
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.FeltWarmCenter
import io.github.smithjustinn.blackjack.ui.theme.LocalShoePosition
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.DragAndDropContainer
import io.github.smithjustinn.blackjack.utils.LocalDragAndDropState
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.delay

import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.side_bet_colored_pair
import sharedui.generated.resources.side_bet_flush
import sharedui.generated.resources.side_bet_mixed_pair
import sharedui.generated.resources.side_bet_perfect_pair
import sharedui.generated.resources.side_bet_result_template
import sharedui.generated.resources.side_bet_straight
import sharedui.generated.resources.side_bet_straight_flush
import sharedui.generated.resources.side_bet_suited_triple
import sharedui.generated.resources.side_bet_three_of_a_kind
import kotlin.random.Random

// GameStatus.isTerminal() is now in GameLogic.kt
// GameState.handResult() is defined in BlackjackHandContainer.kt

@Composable
fun BlackjackScreen(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val appSettings by component.appSettings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showStrategy by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val audioService = LocalAppGraph.current.audioService
    val hapticsService = LocalAppGraph.current.hapticsService
    val animState = remember { BlackjackAnimationState() }
    var headerBalanceOffset by remember { mutableStateOf(Offset.Zero) }

    var selectedAmount by remember { mutableStateOf(10) }
    val onResetBet =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.ResetBet)
                component.onAction(GameAction.ResetSideBets)
            }
        }
    val onDeal =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.FLIP)
                component.onAction(GameAction.Deal)
            }
        }
    val onChipSelected =
        remember(audioService) {
            { amount: Int ->
                selectedAmount = amount
                audioService.playEffect(AudioService.SoundEffect.PLINK)
            }
        }

    LaunchedEffect(appSettings.isSoundMuted) {
        audioService.isMuted = appSettings.isSoundMuted
    }

    val isTerminal by remember { derivedStateOf { state.status.isTerminal() } }
    val showStatus by remember { derivedStateOf { state.status.isStatusVisible() } }
    val isMultiHand by remember { derivedStateOf { state.playerHands.size > 1 } }
    val isBlackjack by remember {
        derivedStateOf {
            state.status == GameStatus.PLAYER_WON && state.playerHands.any { it.isBlackjack }
        }
    }
    var autoDealPending by remember { mutableStateOf(false) }

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

    LaunchedEffect(isTerminal) {
        if (isTerminal) {
            val sideBets = state.lastSideBets
            val rules = appSettings.gameRules
            val handCount = state.handCount
            delay(if (appSettings.isAutoDealEnabled) AnimationConstants.AutoDealDelayTerminalMs else AnimationConstants.ManualResetDelayMs)
            if (appSettings.isAutoDealEnabled) autoDealPending = true
            component.onAction(
                GameAction.NewGame(
                    rules = rules,
                    handCount = handCount,
                    previousBets = state.playerHands.map { it.bet }.toPersistentList(),
                    lastSideBets = sideBets,
                )
            )
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.BETTING && autoDealPending) {
            autoDealPending = false
            if (appSettings.isAutoDealEnabled && state.playerHands.all { it.bet > 0 }) {
                component.onAction(GameAction.Deal)
            } else if (appSettings.isAutoDealEnabled && state.playerHands.any { it.bet == 0 }) {
                component.updateSettings { it.copy(isAutoDealEnabled = false) }
            }
        }
    }

    // Animation orchestration: effects pipeline + state-driven flash/shake
    LaunchedEffect(component) {
        BlackjackAnimationOrchestrator.orchestrate(
            effects = component.effects,
            stateFlow = component.state,
            animState = animState,
            audioService = audioService,
            hapticsService = hapticsService,
        )
    }

    BlackjackTheme {
        DragAndDropContainer {
            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(FeltDeepEdge) // Fallback deep color
                        .drawWithCache {
                            // Static size-dependent brushes — recreated only when size changes
                            val feltBrush =
                                Brush.radialGradient(
                                    // Deeper, richer felt colors for premium feel
                                    colors =
                                        listOf(
                                            FeltWarmCenter,
                                            FeltGreen,
                                            FeltDeepEdge,
                                            Color.Black.copy(alpha = 0.8f)
                                        ),
                                    center = Offset(size.width / 2, size.height * 0.35f),
                                    radius = size.maxDimension * 0.65f
                                )
                            val vignetteBrush =
                                Brush.radialGradient(
                                    // Dramatically deeper vignette for high roller mood
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.85f)
                                        ),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.maxDimension * 0.7f
                                )
                            val arcWidth = size.width * 1.5f
                            val arcHeight = size.height * 0.6f
                            val arcLeft = (size.width - arcWidth) / 2
                            val arcTop = size.height * 0.35f
                            val arcSize =
                                Size(arcWidth, arcHeight)
                            val arcStroke =
                                Stroke(width = 3.dp.toPx())
                            val insuranceStroke =
                                Stroke(width = 1.5.dp.toPx())
                            val insuranceOffset = 40.dp.toPx()

                            onDrawBehind {
                                // 1. Base Felt Gradient (shifted up towards the dealer)
                                drawRect(brush = feltBrush)

                                // 1b. Felt Texture (Subtle Noise / Fibers)
                                val fiberSpacing = 2.dp.toPx()
                                val fiberAlpha = 0.05f
                                for (x in 0..(size.width / fiberSpacing).toInt()) {
                                    drawLine(
                                        color = Color.Black.copy(alpha = fiberAlpha),
                                        start = Offset(x * fiberSpacing, 0f),
                                        end = Offset(x * fiberSpacing, size.height),
                                        strokeWidth = 0.5.dp.toPx()
                                    )
                                }
                                for (y in 0..(size.height / fiberSpacing).toInt()) {
                                    drawLine(
                                        color = Color.Black.copy(alpha = fiberAlpha),
                                        start = Offset(0f, y * fiberSpacing),
                                        end = Offset(size.width, y * fiberSpacing),
                                        strokeWidth = 0.5.dp.toPx()
                                    )
                                }

                                // 2. The Classic Table Arc (Betting Line) - Embossed
                                // Emboss shadow
                                drawArc(
                                    color = Color.Black.copy(alpha = 0.25f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(arcLeft, arcTop + 1.dp.toPx()),
                                    size = arcSize,
                                    style = arcStroke
                                )
                                drawArc(
                                    color = PrimaryGold.copy(alpha = 0.15f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(arcLeft, arcTop),
                                    size = arcSize,
                                    style = arcStroke
                                )

                                // 3. The Insurance Line (Fainter, above the main arc) - Embossed
                                drawArc(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(arcLeft, (arcTop - insuranceOffset) + 1.dp.toPx()),
                                    size = arcSize,
                                    style = insuranceStroke
                                )
                                drawArc(
                                    color = PrimaryGold.copy(alpha = 0.08f),
                                    startAngle = 180f,
                                    sweepAngle = 180f,
                                    useCenter = false,
                                    topLeft = Offset(arcLeft, arcTop - insuranceOffset),
                                    size = arcSize,
                                    style = insuranceStroke
                                )

                                // 4. Heavy Vignette (Simulates the dark leather rail around the table)
                                drawRect(brush = vignetteBrush)

                                // 4b. Physical Table Rail (Wood bumper)
                                val railHeight = 20.dp.toPx()
                                // Bottom Rail
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = listOf(OakMedium, FeltDeepEdge),
                                            startY = size.height - railHeight,
                                            endY = size.height
                                        ),
                                    topLeft = Offset(0f, size.height - railHeight),
                                    size = Size(size.width, railHeight)
                                )
                                // Top Rail
                                drawRect(
                                    brush =
                                        Brush.verticalGradient(
                                            colors = listOf(FeltDeepEdge, OakMedium),
                                            startY = 0f,
                                            endY = railHeight
                                        ),
                                    topLeft = Offset.Zero,
                                    size = Size(size.width, railHeight)
                                )
                                // Rail Highlights
                                drawLine(
                                    color = Color.White.copy(alpha = 0.1f),
                                    start = Offset(0f, railHeight),
                                    end = Offset(size.width, railHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawLine(
                                    color = Color.White.copy(alpha = 0.1f),
                                    start = Offset(0f, size.height - railHeight),
                                    end = Offset(size.width, size.height - railHeight),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }
                        }.drawBehind {
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
                        }.graphicsLayer { translationX = animState.shakeOffset.value * density },
            ) {
                // Table printing added to the felt
                Column(
                    modifier = Modifier.fillMaxSize().padding(top = 180.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Text(
                        text = "BLACKJACK PAYS 3 TO 2",
                        color = PrimaryGold.copy(alpha = 0.12f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 4.sp
                    )
                    Text(
                        text = "Dealer must draw to 16, and stand on all 17s",
                        color = Color.White.copy(alpha = 0.08f),
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "INSURANCE PAYS 2 TO 1",
                        color = PrimaryGold.copy(alpha = 0.08f),
                        style = MaterialTheme.typography.bodySmall,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Enforce a portrait-like aspect ratio (9:16) if the window is too wide (letterboxing)
                val gameModifier =
                    if (maxHeight > maxWidth) {
                        Modifier.fillMaxSize()
                    } else {
                        val gameWidth = maxHeight * (9f / 16f)
                        Modifier.size(gameWidth, maxHeight).align(Alignment.Center)
                    }

                Box(modifier = gameModifier) {
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

                            SideBetResultsOverlay(state = state)

                            // Game Overlays & Status
                            BlackjackGameOverlay(
                                status = state.status,
                                playerHands = state.playerHands,
                                netPayout = state.totalNetPayout(),
                                isBlackjack = isBlackjack,
                                component = component,
                                flashAlphaProvider = { animState.flashAlpha.value },
                                flashColorProvider = { animState.flashColor },
                                showStatus = showStatus,
                                modifier = Modifier.zIndex(5f),
                            )

                            AnimatedVisibility(
                                visible = state.status == GameStatus.BETTING,
                                modifier = Modifier.zIndex(5f),
                                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(tween(AnimationConstants.BettingPhaseEnterDuration)),
                                exit = slideOutVertically(targetOffsetY = { it / 4 }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
                            ) {
                                BettingPhaseScreen(
                                    state = state,
                                    component = component,
                                    audioService = audioService,
                                    selectedAmount = selectedAmount,
                                )
                            }

                            AnimatedVisibility(
                                visible = showSettings,
                                modifier = Modifier.zIndex(10f),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
                            ) {
                                SettingsOverlay(
                                    settings = appSettings,
                                    onUpdateSettings = component::updateSettings,
                                    onResetBalance = component::resetBalance,
                                    onDismiss = onDismissSettings
                                )
                            }

                            AnimatedVisibility(
                                visible = showRules,
                                modifier = Modifier.zIndex(10f),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
                            ) {
                                RulesOverlay(
                                    onDismiss = onDismissRules
                                )
                            }

                            for (i in 0 until animState.chipEruptions.size) {
                                val instance = animState.chipEruptions[i]
                                key(instance.id) {
                                    ChipEruptionEffect(
                                        amount = instance.amount,
                                        startOffset = instance.startOffset
                                    )
                                }
                            }
                            for (i in 0 until animState.chipLosses.size) {
                                val instance = animState.chipLosses[i]
                                key(instance.id) {
                                    ChipLossEffect(amount = instance.amount)
                                }
                            }

                            for (i in 0 until animState.activePayouts.size) {
                                val instance = animState.activePayouts[i]
                                key(instance.id) {
                                    PayoutEffect(
                                        amount = instance.amount,
                                        targetOffset = instance.targetOffset,
                                        onAnimationEnd = { animState.activePayouts.remove(instance) }
                                    )
                                }
                            }
                        }
                    } // CompositionLocalProvider
                }

                AnimatedVisibility(
                    visible = showStrategy,
                    modifier = Modifier.zIndex(10f),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
                ) {
                    StrategyGuideOverlay(
                        onDismiss = onDismissStrategy
                    )
                }
            } // End of gameModifier Box
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

@Composable
private fun BlackjackGameOverlay(
    status: GameStatus,
    playerHands: List<Hand>,
    netPayout: Int?,
    isBlackjack: Boolean,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
    flashColorProvider: () -> Color,
    showStatus: Boolean,
    modifier: Modifier = Modifier,
) {
    val onTakeInsurance = remember(component) { { component.onAction(GameAction.TakeInsurance) } }
    val onDeclineInsurance = remember(component) { { component.onAction(GameAction.DeclineInsurance) } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = showStatus,
            enter =
                fadeIn(animationSpec = tween(AnimationConstants.StatusMessageEnterDuration)) +
                    scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)),
            exit = fadeOut(animationSpec = tween(AnimationConstants.StatusMessageExitDuration)) + scaleOut(targetScale = 0.8f),
        ) {
            GameStatusMessage(status = status, netPayout = netPayout, isBlackjack = isBlackjack)
        }

        if (status == GameStatus.INSURANCE_OFFERED) {
            InsuranceOverlay(
                onInsure = onTakeInsurance,
                onDecline = onDeclineInsurance,
            )
        }

        if (status == GameStatus.PLAYER_WON) {
            ConfettiEffect(
                particleCount = if (isBlackjack) 250 else 120,
                isBlackjack = isBlackjack,
            )
        }

        if (status == GameStatus.PLAYER_WON && isBlackjack) {
            SparkleEffect()
        }
        // ChipEruption is handled via chipEruptions state list triggered by GameEffect

        // Bolt Performance Optimization: Defer state read to draw phase to prevent O(Frames) recompositions
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val alpha = flashAlphaProvider()
                        if (alpha > 0f) {
                            drawRect(flashColorProvider().copy(alpha = alpha))
                        }
                    },
        )
    }
}

@Composable
private fun BlackjackLayout(
    state: GameState,
    component: BlackjackComponent,
    dealRegistry: DealAnimationRegistry,
) {
    val shoePosition = remember { mutableStateOf<Offset?>(null) }
    val density = LocalDensity.current

    // Track all cards to detect additions and face-down changes.
    // Keyed on dealerHand.cards and playerHands so the list identity only changes when card
    // content actually changes, preventing computeTableLayout from running on unrelated state
    // updates (balance, status, etc.).
    val allCards =
        remember(state.dealerHand.cards, state.playerHands) {
            var totalSize = state.dealerHand.cards.size
            for (i in 0 until state.playerHands.size) {
                totalSize += state.playerHands[i].cards.size
            }
            val cards = ArrayList<Card>(totalSize)
            for (i in 0 until state.dealerHand.cards.size) {
                cards.add(state.dealerHand.cards[i])
            }
            for (i in 0 until state.playerHands.size) {
                val handCards = state.playerHands[i].cards
                for (j in 0 until handCards.size) {
                    cards.add(handCards[j])
                }
            }
            cards
        }

    CompositionLocalProvider(LocalShoePosition provides shoePosition) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val areaW = constraints.maxWidth.toFloat()
            val areaH = constraints.maxHeight.toFloat()

            // Recompute layout whenever cards change (deals or reveals) or layout bounds change
            LaunchedEffect(allCards, areaW, areaH, density, shoePosition.value) {
                val currentShoePos = shoePosition.value ?: Offset(areaW, -100f)
                val layout = computeTableLayout(state, areaW, areaH, density, currentShoePos)
                dealRegistry.tableLayout = layout
            }

            // Shoe widget in top-right corner
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInRoot()
                            shoePosition.value =
                                Offset(
                                    pos.x + coords.size.width / 2f,
                                    pos.y + coords.size.height / 2f,
                                )
                        }
            ) {
                Shoe(
                    state = state,
                    modifier =
                        Modifier
                            .padding(top = 16.dp, end = 16.dp)
                            .graphicsLayer {
                                rotationZ = -15f
                                rotationX = 10f
                                translationX = 20.dp.toPx()
                                translationY = -10.dp.toPx()
                            }
                )
            }
        }
    }
}

// PayoutInstance, ChipEruptionInstance, ChipLossInstance are defined in
// ui/animation/BlackjackAnimationState.kt

@Composable
private fun SideBetResultsOverlay(state: GameState) {
    AnimatedVisibility(
        visible = state.sideBetResults.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.StatusMessageEnterDuration)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.StatusMessageExitDuration)),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
                .zIndex(4f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                state.sideBetResults.forEach { (_, result) ->
                    Box(
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .border(1.dp, PrimaryGold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.side_bet_result_template,
                                    getLocalizedOutcomeName(result.outcomeName),
                                    result.payoutAmount,
                                ),
                            color = PrimaryGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getLocalizedOutcomeName(name: String): String {
    return when (name) {
        "Perfect Pair" -> stringResource(Res.string.side_bet_perfect_pair)
        "Colored Pair" -> stringResource(Res.string.side_bet_colored_pair)
        "Mixed Pair" -> stringResource(Res.string.side_bet_mixed_pair)
        "Suited Triple" -> stringResource(Res.string.side_bet_suited_triple)
        "Straight Flush" -> stringResource(Res.string.side_bet_straight_flush)
        "Three of a Kind" -> stringResource(Res.string.side_bet_three_of_a_kind)
        "Straight" -> stringResource(Res.string.side_bet_straight)
        "Flush" -> stringResource(Res.string.side_bet_flush)
        else -> name
    }
}
