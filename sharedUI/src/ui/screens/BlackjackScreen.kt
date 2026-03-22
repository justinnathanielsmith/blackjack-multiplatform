package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
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
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
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
import io.github.smithjustinn.blackjack.ui.components.totalNetPayout
import io.github.smithjustinn.blackjack.ui.effects.ChipEruptionEffect
import io.github.smithjustinn.blackjack.ui.effects.ChipLossEffect
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.SparkleEffect
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDeepEdge
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.FeltWarmCenter
import io.github.smithjustinn.blackjack.ui.theme.LocalShoePosition
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.DragAndDropContainer
import io.github.smithjustinn.blackjack.utils.LocalDragAndDropState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
// GameState.handResult() is defined in OverlayCardTable.kt

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
    var nearMissHandIndex by remember { mutableStateOf<Int?>(null) }
    var headerBalanceOffset by remember { mutableStateOf(Offset.Zero) }
    // List of active chip eruption instances
    val chipEruptions = remember { mutableStateListOf<ChipEruptionInstance>() }
    val chipLosses = remember { mutableStateListOf<ChipLossInstance>() }

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
    val isMultiHand by remember { derivedStateOf { state.playerHands.size > 1 } }
    val isBlackjack by remember {
        derivedStateOf {
            state.status == GameStatus.PLAYER_WON && state.playerHands.any { it.cards.size == 2 && it.score == 21 }
        }
    }
    var flashColor by remember { mutableStateOf(Color.White) }
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

    val activePayouts = remember { mutableStateListOf<PayoutInstance>() }
    val dealRegistry = remember { DealAnimationRegistry() }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.BETTING) {
            dealRegistry.tableLayout = null
        }
    }

    // handZones[0] = dealer, handZones[1..N] = player hands
    val activeHandHighlightPosition by animateOffsetAsState(
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
            state.playerHands.forEachIndexed { index, _ ->
                val result = state.handResult(index)
                if (result == HandResult.WIN) {
                    val bet = state.playerHands.getOrNull(index)?.bet ?: 0
                    if (bet > 0) {
                        delay(200)
                        val zone = dealRegistry.tableLayout?.handZones?.getOrNull(index + 1)
                        val target =
                            if (zone != null) {
                                zone.clusterCenter + dealRegistry.gameplayAreaOffset
                            } else {
                                Offset.Zero
                            }
                        activePayouts.add(PayoutInstance(Random.nextLong(), bet, target))
                    }
                }
            }
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.PLAYER_WON) {
            if (isBlackjack) {
                flashColor = PrimaryGold
                flashAlpha.animateTo(0.25f, tween(100))
                flashAlpha.animateTo(0f, tween(300))
            } else {
                flashColor = Color.White
                flashAlpha.animateTo(0.15f, tween(100))
                flashAlpha.animateTo(0f, tween(400))
            }
        }
    }

    LaunchedEffect(isTerminal) {
        if (isTerminal) {
            val bet = state.currentBet
            val sideBets = state.sideBets
            val rules = appSettings.gameRules
            val handCount = state.handCount
            delay(if (appSettings.isAutoDealEnabled) 1500L else 2000L)
            if (appSettings.isAutoDealEnabled) autoDealPending = true
            component.onAction(
                GameAction.NewGame(
                    rules = rules,
                    handCount = handCount,
                    lastBet = bet,
                    lastSideBets = sideBets,
                )
            )
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.BETTING && autoDealPending) {
            autoDealPending = false
            if (appSettings.isAutoDealEnabled && state.currentBets.all { it > 0 }) {
                component.onAction(GameAction.Deal)
            } else if (appSettings.isAutoDealEnabled && state.currentBets.any { it == 0 }) {
                component.updateSettings { it.copy(isAutoDealEnabled = false) }
            }
        }
    }

    LaunchedEffect(component) {
        component.effects.collect { effect: GameEffect ->
            handleGameEffect(
                effect = effect,
                hapticsService = hapticsService,
                audioService = audioService
            )
            if (effect is GameEffect.NearMissHighlight) {
                launch {
                    nearMissHandIndex = effect.handIndex
                    delay(1500L)
                    nearMissHandIndex = null
                }
            }
            if (effect is GameEffect.ChipEruption) {
                launch {
                    val instance =
                        ChipEruptionInstance(
                            id = Random.nextLong(),
                            amount = effect.amount,
                            startOffset = null
                        )
                    chipEruptions.add(instance)
                    delay(3000L) // Duration of effect
                    chipEruptions.remove(instance)
                }
            }
            if (effect is GameEffect.ChipLoss) {
                launch {
                    val instance = ChipLossInstance(id = Random.nextLong(), amount = effect.amount)
                    chipLosses.add(instance)
                    delay(3000L) // Duration of effect
                    chipLosses.remove(instance)
                }
            }
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
                                    colors = listOf(FeltWarmCenter, FeltGreen, FeltDeepEdge),
                                    center = Offset(size.width / 2, size.height * 0.35f),
                                    radius = size.maxDimension * 0.6f
                                )
                            val vignetteBrush =
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.maxDimension * 0.55f
                                )
                            val arcWidth = size.width * 1.5f
                            val arcHeight = size.height * 0.6f
                            val arcLeft = (size.width - arcWidth) / 2
                            val arcTop = size.height * 0.35f
                            val arcSize =
                                androidx.compose.ui.geometry
                                    .Size(arcWidth, arcHeight)
                            val arcStroke =
                                androidx.compose.ui.graphics.drawscope
                                    .Stroke(width = 3.dp.toPx())
                            val insuranceStroke =
                                androidx.compose.ui.graphics.drawscope
                                    .Stroke(width = 1.5.dp.toPx())
                            val insuranceOffset = 40.dp.toPx()
                            val highlightRadius = size.maxDimension * 0.4f

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

                                // 5. Active Hand Highlight — read in draw scope to avoid recomposition
                                val highlightPos = activeHandHighlightPosition
                                if (highlightPos != Offset.Zero) {
                                    drawRect(
                                        brush =
                                            Brush.radialGradient(
                                                colors = listOf(PrimaryGold.copy(alpha = 0.08f), Color.Transparent),
                                                center = highlightPos,
                                                radius = highlightRadius
                                            )
                                    )
                                }
                            }
                        }.graphicsLayer { translationX = shakeOffset.value * density },
            ) {
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
                                nearMissHandIndex = nearMissHandIndex,
                                modifier = Modifier.zIndex(1f),
                            )

                            SideBetResultsOverlay(state = state)

                            // Game Status Overlay (On top of hands)
                            val showStatus by remember {
                                derivedStateOf {
                                    state.status != GameStatus.PLAYING &&
                                        state.status != GameStatus.BETTING &&
                                        state.status != GameStatus.INSURANCE_OFFERED &&
                                        state.status != GameStatus.IDLE
                                }
                            }

                            // Game Overlays & Status
                            BlackjackGameOverlay(
                                status = state.status,
                                playerHands = state.playerHands,
                                netPayout = state.totalNetPayout(),
                                isBlackjack = isBlackjack,
                                component = component,
                                flashAlphaProvider = { flashAlpha.value },
                                flashColorProvider = { flashColor },
                                showStatus = showStatus,
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
                                    selectedAmount = selectedAmount,
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
                                    onResetBalance = component::resetBalance,
                                    onDismiss = onDismissSettings
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = showRules,
                                modifier = Modifier.zIndex(10f),
                                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
                            ) {
                                RulesOverlay(
                                    onDismiss = onDismissRules
                                )
                            }

                            chipEruptions.forEach { instance ->
                                key(instance.id) {
                                    ChipEruptionEffect(
                                        amount = instance.amount,
                                        startOffset = instance.startOffset
                                    )
                                }
                            }
                            chipLosses.forEach { instance ->
                                key(instance.id) {
                                    ChipLossEffect(amount = instance.amount)
                                }
                            }

                            activePayouts.forEach { instance ->
                                key(instance.id) {
                                    io.github.smithjustinn.blackjack.ui.effects.PayoutEffect(
                                        amount = instance.amount,
                                        targetOffset = instance.targetOffset,
                                        onAnimationEnd = { activePayouts.remove(instance) }
                                    )
                                }
                            }
                        }
                    } // CompositionLocalProvider
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = showStrategy,
                    modifier = Modifier.zIndex(10f),
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(300)),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(200)),
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
    playerHands: List<io.github.smithjustinn.blackjack.Hand>,
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
        androidx.compose.animation.AnimatedVisibility(
            visible = showStatus,
            enter =
                fadeIn(animationSpec = tween(200)) +
                    scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)),
            exit = fadeOut(animationSpec = tween(150)) + scaleOut(targetScale = 0.8f),
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

        val flashAlpha = flashAlphaProvider()
        if (flashAlpha > 0f) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(flashColorProvider().copy(alpha = flashAlphaProvider()))
                        },
            )
        }
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

    // Track all cards to detect additions and face-down changes
    val allCards =
        remember(state) {
            state.dealerHand.cards + state.playerHands.flatMap { it.cards }
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

data class PayoutInstance(
    val id: Long,
    val amount: Int,
    val targetOffset: Offset
)

data class ChipEruptionInstance(
    val id: Long,
    val amount: Int,
    val startOffset: Offset?
)

data class ChipLossInstance(
    val id: Long,
    val amount: Int
)

@Composable
private fun SideBetResultsOverlay(state: GameState) {
    AnimatedVisibility(
        visible = state.status == GameStatus.PLAYING && state.sideBetResults.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(150)),
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
