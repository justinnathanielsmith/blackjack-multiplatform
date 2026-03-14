package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import io.github.smithjustinn.blackjack.ui.components.HandRow
import io.github.smithjustinn.blackjack.ui.components.Header
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.handleGameEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import kotlinx.coroutines.launch

@Composable
fun BlackjackScreen(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val audioService = LocalAppGraph.current.audioService
    val hapticsService = LocalAppGraph.current.hapticsService
    val shakeOffset = remember { Animatable(0f) }

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

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulseScale",
    )

    BlackjackTheme {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to FeltGreen,
                            1.0f to FeltDark,
                            radius = 2000f,
                        ),
                    ).offset(x = shakeOffset.value.dp),
        ) {
            val isLandscape = maxWidth > maxHeight
            val isCompactHeight = maxHeight < 500.dp
            val useCompactUI = isLandscape && isCompactHeight

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
            ) {
                Header(balance = state.balance)

                Box(modifier = Modifier.weight(1f)) {
                    if (state.status == GameStatus.PLAYER_WON) {
                        ConfettiEffect()
                    }

                    if (state.status == GameStatus.BETTING) {
                        BettingPhaseScreen(
                            state = state,
                            component = component,
                            audioService = audioService,
                            isCompact = useCompactUI,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (useCompactUI) {
                        LandscapeLayout(
                            state = state,
                            audioService = audioService,
                            component = component,
                            pulseScale = pulseScale,
                        )
                    } else {
                        PortraitLayout(
                            state = state,
                            audioService = audioService,
                            component = component,
                            pulseScale = pulseScale,
                        )
                    }

                    if (state.status == GameStatus.INSURANCE_OFFERED) {
                        InsuranceOverlay(
                            onInsure = { component.onAction(GameAction.TakeInsurance) },
                            onDecline = { component.onAction(GameAction.DeclineInsurance) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        val dealerDisplayScore =
            if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
        HandContainer(title = "Dealer", score = dealerDisplayScore) {
            HandRow(state.dealerHand)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.status != GameStatus.PLAYING &&
            state.status != GameStatus.BETTING &&
            state.status != GameStatus.INSURANCE_OFFERED
        ) {
            GameStatusMessage(status = state.status, pulseScale = pulseScale, isCompact = false)
        }

        Spacer(modifier = Modifier.weight(1f))

        val splitHand = state.splitHand
        if (splitHand != null) {
            val primaryActive = !state.isPlayingSplitHand && state.status == GameStatus.PLAYING
            val splitActive = state.isPlayingSplitHand && state.status == GameStatus.PLAYING

            HandContainer(
                title = "Hand 1",
                score = state.playerHand.score,
                bet = state.currentBet,
                isActive = primaryActive,
                isPending = !primaryActive && state.status == GameStatus.PLAYING,
            ) {
                HandRow(state.playerHand)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HandContainer(
                title = "Hand 2",
                score = splitHand.score,
                bet = state.splitBet,
                isActive = splitActive,
                isPending = !splitActive && state.status == GameStatus.PLAYING,
            ) {
                HandRow(splitHand)
            }
        } else {
            HandContainer(
                title = "You",
                score = state.playerHand.score,
                bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                isActive = state.status == GameStatus.PLAYING,
            ) {
                HandRow(state.playerHand)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        GameActions(
            state = state,
            audioService = audioService,
            component = component,
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float,
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Left side: Cards
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            val dealerDisplayScore =
                if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
            HandContainer(title = "Dealer", score = dealerDisplayScore) {
                HandRow(state.dealerHand)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val splitHand = state.splitHand
            if (splitHand != null) {
                val primaryActive = !state.isPlayingSplitHand && state.status == GameStatus.PLAYING
                val splitActive = state.isPlayingSplitHand && state.status == GameStatus.PLAYING

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HandContainer(
                        title = "H1",
                        score = state.playerHand.score,
                        bet = state.currentBet,
                        isActive = primaryActive,
                        isPending = !primaryActive && state.status == GameStatus.PLAYING,
                        modifier = Modifier.weight(1f),
                    ) {
                        HandRow(state.playerHand)
                    }

                    HandContainer(
                        title = "H2",
                        score = splitHand.score,
                        bet = state.splitBet,
                        isActive = splitActive,
                        isPending = !splitActive && state.status == GameStatus.PLAYING,
                        modifier = Modifier.weight(1f),
                    ) {
                        HandRow(splitHand)
                    }
                }
            } else {
                HandContainer(
                    title = "You",
                    score = state.playerHand.score,
                    bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                    isActive = state.status == GameStatus.PLAYING,
                ) {
                    HandRow(state.playerHand)
                }
            }
        }

        // Right side: Status and Actions
        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (state.status != GameStatus.PLAYING &&
                state.status != GameStatus.BETTING &&
                state.status != GameStatus.INSURANCE_OFFERED
            ) {
                GameStatusMessage(status = state.status, pulseScale = pulseScale, isCompact = true)
                Spacer(modifier = Modifier.height(16.dp))
            }

            GameActions(
                state = state,
                audioService = audioService,
                component = component,
            )
        }
    }
}
