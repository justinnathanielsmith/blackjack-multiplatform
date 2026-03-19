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
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.BlackjackRules
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.HandOutcome
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.ControlCenter
import io.github.smithjustinn.blackjack.ui.components.DealerHand
import io.github.smithjustinn.blackjack.ui.components.GameStatusMessage
import io.github.smithjustinn.blackjack.ui.components.HandResult
import io.github.smithjustinn.blackjack.ui.components.HandStatus
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.components.PlayerHand
import io.github.smithjustinn.blackjack.ui.components.RulesOverlay
import io.github.smithjustinn.blackjack.ui.components.SettingsOverlay
import io.github.smithjustinn.blackjack.ui.components.Shoe
import io.github.smithjustinn.blackjack.ui.components.StrategyGuideOverlay
import io.github.smithjustinn.blackjack.ui.effects.ChipEruptionEffect
import io.github.smithjustinn.blackjack.ui.effects.ChipLossEffect
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDeepEdge
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.FeltWarmCenter
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.dealer
import sharedui.generated.resources.hand_number
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

fun GameState.handResult(index: Int): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val hand = playerHands.getOrNull(index) ?: return HandResult.NONE
    return when (BlackjackRules.determineHandOutcome(hand, dealerHand.score, dealerHand.isBust)) {
        HandOutcome.WIN, HandOutcome.NATURAL_WIN -> HandResult.WIN
        HandOutcome.PUSH -> HandResult.PUSH
        HandOutcome.LOSS -> HandResult.LOSS
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
    var nearMissHandIndex by remember { mutableStateOf<Int?>(null) }
    var headerBalanceOffset by remember { mutableStateOf(Offset.Zero) }
    // List of active chip eruption instances
    val chipEruptions = remember { mutableStateListOf<ChipEruptionInstance>() }
    val chipLosses = remember { mutableStateListOf<Int>() }

    LaunchedEffect(appSettings.isSoundMuted) {
        audioService.isMuted = appSettings.isSoundMuted
    }

    val isTerminal by remember(state.status) { derivedStateOf { state.status.isTerminal() } }
    val isMultiHand by remember(state.playerHands.size) { derivedStateOf { state.playerHands.size > 1 } }
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
    val handBetOffsets = remember { mutableStateMapOf<Int, Offset>() }

    val activeHandHighlightPosition by animateOffsetAsState(
        targetValue =
            if (state.status == GameStatus.PLAYING) {
                handBetOffsets[state.activeHandIndex] ?: Offset.Zero
            } else {
                Offset.Zero
            },
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "activeHandHighlight"
    )

    // Trigger payout animations when results are calculated and it's a win
    LaunchedEffect(state.status, state.playerHands) {
        if (state.status.isTerminal()) {
            state.playerHands.forEachIndexed { index, _ ->
                val result = state.handResult(index)
                if (result == HandResult.WIN) {
                    val bet = state.playerBets.getOrNull(index) ?: 0
                    if (bet > 0) {
                        // Delay slightly so it doesn't happen instantly with the result badge
                        delay(200)
                        val target = handBetOffsets[index] ?: Offset.Zero
                        activePayouts.add(PayoutInstance(Random.nextLong(), bet, target))
                    }
                }
            }
        }
    }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.PLAYER_WON) {
            flashAlpha.animateTo(0.15f, tween(100))
            flashAlpha.animateTo(0f, tween(400))
        }
    }

    LaunchedEffect(isTerminal) {
        if (isTerminal) {
            val bet = state.currentBet
            val sideBets = state.sideBets
            val rules = appSettings.gameRules
            val handCount = appSettings.defaultHandCount
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
            if (appSettings.isAutoDealEnabled && state.currentBet > 0) {
                component.onAction(GameAction.Deal)
            } else if (appSettings.isAutoDealEnabled && state.currentBet == 0) {
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
                    chipLosses.add(effect.amount)
                    delay(3000L) // Duration of effect
                    chipLosses.remove(effect.amount)
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

    // Update the background brush to center the light source slightly higher (where the dealer is)
    val backgroundBrush =
        remember(FeltWarmCenter, FeltGreen, FeltDeepEdge) {
            Brush.radialGradient(
                0.0f to FeltWarmCenter,
                0.6f to FeltGreen,
                1.0f to FeltDeepEdge,
                center = Offset.Unspecified // defaults to center; tweaked in canvas below
            )
        }

    BlackjackTheme {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(FeltDeepEdge) // Fallback deep color
                    .drawBehind {
                        // 1. Base Felt Gradient (shifted up towards the dealer)
                        drawRect(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(FeltWarmCenter, FeltGreen, FeltDeepEdge),
                                    center = Offset(size.width / 2, size.height * 0.35f),
                                    radius = size.maxDimension * 0.6f
                                )
                        )

                        // 2. The Classic Table Arc (Betting Line)
                        val arcWidth = size.width * 1.5f
                        val arcHeight = size.height * 0.6f
                        val arcLeft = (size.width - arcWidth) / 2
                        val arcTop = size.height * 0.35f // Starts below the dealer

                        drawArc(
                            color = PrimaryGold.copy(alpha = 0.15f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(arcLeft, arcTop),
                            size =
                                androidx.compose.ui.geometry
                                    .Size(arcWidth, arcHeight),
                            style =
                                androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 3.dp.toPx()
                                )
                        )

                        // 3. The Insurance Line (Fainter, above the main arc)
                        drawArc(
                            color = PrimaryGold.copy(alpha = 0.08f),
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(arcLeft, arcTop - 40.dp.toPx()),
                            size =
                                androidx.compose.ui.geometry
                                    .Size(arcWidth, arcHeight),
                            style =
                                androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 1.5.dp.toPx()
                                )
                        )

                        // 4. Heavy Vignette (Simulates the dark leather rail around the table)
                        drawRect(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                                    center = Offset(size.width / 2, size.height / 2),
                                    radius = size.maxDimension * 0.55f
                                )
                        )

                        // 5. Active Hand Highlight (Subtle)
                        if (activeHandHighlightPosition != Offset.Zero) {
                            drawRect(
                                brush =
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                PrimaryGold.copy(alpha = 0.08f),
                                                Color.Transparent
                                            ),
                                        center = activeHandHighlightPosition,
                                        radius = size.maxDimension * 0.4f
                                    )
                            )
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
                                .fillMaxSize(),
                    ) {
                        BlackjackLayout(
                            state = state,
                            component = component,
                            nearMissHandIndex = nearMissHandIndex,
                            handBetOffsets = handBetOffsets,
                        )
                    }

                    ControlCenter(
                        state = state,
                        component = component,
                        isCompact = isMultiHand,
                    )
                }

                // Overlay layer (full-bleed within the game bounds)
                Box(modifier = Modifier.fillMaxSize()) {
                    // Game Status Overlay (On top of hands)
                    val showStatus by remember(state.status) {
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
                        component = component,
                        flashAlphaProvider = { flashAlpha.value },
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
                    chipLosses.forEach { amount ->
                        ChipLossEffect(amount = amount)
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
        }
    }
}

@Composable
private fun BlackjackGameOverlay(
    status: GameStatus,
    playerHands: List<io.github.smithjustinn.blackjack.Hand>,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
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
            enter = fadeIn() + scaleIn(initialScale = 0.8f),
            exit = fadeOut() + scaleOut(targetScale = 0.8f),
        ) {
            GameStatusMessage(status = status)
        }

        if (status == GameStatus.INSURANCE_OFFERED) {
            InsuranceOverlay(
                onInsure = onTakeInsurance,
                onDecline = onDeclineInsurance,
            )
        }

        if (status == GameStatus.PLAYER_WON) {
            val isBlackjack by remember(playerHands) {
                derivedStateOf { playerHands.any { it.cards.size == 2 && it.score == 21 } }
            }
            ConfettiEffect(
                particleCount = if (isBlackjack) 250 else 120,
            )
        }
        // ChipEruption is handled via chipEruptions state list triggered by GameEffect

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
private fun BlackjackLayout(
    state: GameState,
    component: BlackjackComponent,
    nearMissHandIndex: Int? = null,
    handBetOffsets: MutableMap<Int, Offset>,
) {
    val hands = state.playerHands
    val handCount = hands.size

    val baseCardScale =
        when (handCount) {
            1 -> 1.0f
            2 -> 0.80f
            else -> 0.55f
        }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. The Shoe (Deck) in the top-right corner
        Shoe(
            state = state,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .graphicsLayer {
                        // Tilt the shoe slightly to fit the table's perspective
                        rotationZ = -15f
                        rotationX = 10f
                        translationX = 20.dp.toPx()
                        translationY = -10.dp.toPx()
                    }
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dealerDisplayScore =
                remember(state.status, state.dealerHand) {
                    if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
                }

            // Dealer Hand - Fixed size at the top
            DealerHand(
                hand = state.dealerHand,
                score = dealerDisplayScore,
                title = stringResource(Res.string.dealer),
                isCompact = handCount > 1,
                isExtraCompact = handCount > 2,
                isSlowReveal = state.dealerDrawIsCritical,
                scale = baseCardScale,
            )

            // Player Hands - Dynamic space between dealer and actions
            DynamicPlayerHandsLayout(
                state = state,
                component = component,
                baseCardScale = baseCardScale,
                onBetPositioned = { index, offset ->
                    handBetOffsets[index] = offset
                }
            )

            // Action Bar moved to ControlCenter
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

@Composable
private fun ColumnScope.DynamicPlayerHandsLayout(
    state: GameState,
    component: BlackjackComponent,
    baseCardScale: Float,
    onBetPositioned: (Int, Offset) -> Unit,
) {
    val hands = state.playerHands
    val handCount = hands.size

    if (handCount == 1) {
        // Single hand: Centered vertically and scaled up
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val hand = hands[0]
            val isActive =
                remember(state.activeHandIndex, state.status) {
                    state.activeHandIndex == 0 && state.status == GameStatus.PLAYING
                }
            val status =
                when {
                    hand.isBust -> HandStatus.BUSTED
                    isActive -> HandStatus.ACTIVE
                    else -> HandStatus.WAITING
                }

            PlayerHand(
                handTotal = hand.score,
                status = status,
                cards = hand.cards,
                result = state.handResult(0),
                modifier = Modifier, // no fillMaxHeight, allowing natural centering
                scale = baseCardScale,
                isCompact = false,
                isExtraCompact = false,
                showStatus = false,
                onPositioned = { onBetPositioned(0, it) }
            )

            SideBetResultsOverlay(state = state)
        }
    } else {
        // Multi-hand: Distribute visually with weight modifiers
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            hands.forEachIndexed { index, hand ->
                val isActive =
                    remember(index, state.activeHandIndex, state.status) {
                        index == state.activeHandIndex && state.status == GameStatus.PLAYING
                    }
                val status =
                    when {
                        hand.isBust -> HandStatus.BUSTED
                        isActive -> HandStatus.ACTIVE
                        else -> HandStatus.WAITING
                    }

                // Currently active hand gets slightly more vertical space
                val layoutWeight = if (isActive) 1.15f else 1.0f

                Box(
                    modifier = Modifier.weight(layoutWeight),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerHand(
                        handTotal = hand.score,
                        status = status,
                        cards = hand.cards,
                        result = state.handResult(index),
                        title = stringResource(Res.string.hand_number, index + 1),
                        modifier = Modifier,
                        scale = baseCardScale,
                        isCompact = true,
                        isExtraCompact = handCount > 2,
                        onPositioned = { onBetPositioned(index, it) }
                    )

                    if (index == 0) {
                        SideBetResultsOverlay(state = state)
                    }
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SideBetResultsOverlay(state: GameState) {
    androidx.compose.animation.AnimatedVisibility(
        visible = state.status == GameStatus.PLAYING && state.sideBetResults.isNotEmpty(),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(200)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(150)),
        modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .zIndex(4f)
                .padding(bottom = 8.dp),
    ) {
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
                                result.payoutAmount
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
