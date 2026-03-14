package io.github.smithjustinn.blackjack.ui

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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.SplitHandIndicator
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.DeepFeltGreen
import io.github.smithjustinn.blackjack.ui.theme.DeepFeltGreenDark
import io.github.smithjustinn.blackjack.ui.theme.GlassBlack
import io.github.smithjustinn.blackjack.ui.theme.GlassWhite
import io.github.smithjustinn.blackjack.ui.theme.ModernGold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.deal
import sharedui.generated.resources.hit
import sharedui.generated.resources.split
import sharedui.generated.resources.stand
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_idle
import sharedui.generated.resources.status_player_won
import sharedui.generated.resources.status_playing
import sharedui.generated.resources.status_push

@Composable
fun BlackjackContent(component: BlackjackComponent) {
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
                repeatMode = RepeatMode.Reverse
            ),
        label = "pulseScale"
    )

    BlackjackTheme {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to DeepFeltGreen,
                            0.7f to DeepFeltGreenDark,
                            1.0f to Color.Black,
                            radius = 2000f
                        )
                    ).offset(x = shakeOffset.value.dp)
        ) {
            val isLandscape = maxWidth > maxHeight
            val isCompactHeight = maxHeight < 500.dp
            val useCompactUI = isLandscape && isCompactHeight

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
            ) {
                if (state.status == GameStatus.PLAYER_WON) {
                    ConfettiEffect()
                }

                if (state.status == GameStatus.BETTING) {
                    BettingPhaseContent(
                        state = state,
                        component = component,
                        audioService = audioService,
                        isCompact = useCompactUI,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (useCompactUI) {
                    LandscapeLayout(
                        state = state,
                        audioService = audioService,
                        component = component,
                        pulseScale = pulseScale
                    )
                } else {
                    PortraitLayout(
                        state = state,
                        audioService = audioService,
                        component = component,
                        pulseScale = pulseScale
                    )
                }

                if (state.status == GameStatus.INSURANCE_OFFERED) {
                    InsuranceOverlay(
                        onInsure = { component.onAction(GameAction.TakeInsurance) },
                        onDecline = { component.onAction(GameAction.DeclineInsurance) }
                    )
                }
            }
        }
    }
}

@Composable
private fun InsuranceOverlay(
    onInsure: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(GlassBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, GlassWhite, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INSURANCE?",
                style = MaterialTheme.typography.headlineMedium,
                color = ModernGold,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dealer shows an ACE. Insurance pays 2:1.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CasinoButton(
                    text = "NO THANKS",
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
                CasinoButton(
                    text = "INSURE",
                    onClick = onInsure,
                    modifier = Modifier.weight(1f),
                    isStrategic = true
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        val dealerDisplayScore =
            if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
        Text(
            text = "DEALER: $dealerDisplayScore",
            style = MaterialTheme.typography.titleMedium,
            color = ModernGold,
            fontWeight = FontWeight.Bold
        )
        HandRow(state.dealerHand)

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

            SplitHandIndicator(
                isActive = primaryActive,
                isResolved = state.isPlayingSplitHand || state.status != GameStatus.PLAYING
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "PRIMARY: ${state.playerHand.score}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ModernGold
                    )
                    HandRow(state.playerHand)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            SplitHandIndicator(isActive = splitActive, isResolved = state.status != GameStatus.PLAYING) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SPLIT: ${splitHand.score}",
                        style = MaterialTheme.typography.titleSmall,
                        color = ModernGold
                    )
                    HandRow(splitHand)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "YOU: ${state.playerHand.score}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernGold,
                    fontWeight = FontWeight.Bold
                )
                HandRow(state.playerHand)
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        GameActions(
            state = state,
            audioService = audioService,
            component = component,
            isCompact = false
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: Cards
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val dealerDisplayScore =
                if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
            Text(
                text = "DEALER: $dealerDisplayScore",
                style = MaterialTheme.typography.titleMedium,
                color = ModernGold,
                fontWeight = FontWeight.Bold
            )
            HandRow(state.dealerHand)

            Spacer(modifier = Modifier.height(16.dp))

            val splitHand = state.splitHand
            if (splitHand != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val primaryActive = !state.isPlayingSplitHand && state.status == GameStatus.PLAYING
                    val splitActive = state.isPlayingSplitHand && state.status == GameStatus.PLAYING

                    SplitHandIndicator(
                        isActive = primaryActive,
                        isResolved = state.isPlayingSplitHand || state.status != GameStatus.PLAYING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "P: ${state.playerHand.score}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ModernGold
                            )
                            HandRow(state.playerHand)
                        }
                    }

                    SplitHandIndicator(
                        isActive = splitActive,
                        isResolved = state.status != GameStatus.PLAYING,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "S: ${splitHand.score}",
                                style = MaterialTheme.typography.titleSmall,
                                color = ModernGold
                            )
                            HandRow(splitHand)
                        }
                    }
                }
            } else {
                Text(
                    text = "YOU: ${state.playerHand.score}",
                    style = MaterialTheme.typography.titleMedium,
                    color = ModernGold,
                    fontWeight = FontWeight.Bold
                )
                HandRow(state.playerHand)
            }
        }

        // Right side: Status and Actions
        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                isCompact = true
            )
        }
    }
}

@Composable
private fun GameStatusMessage(
    status: GameStatus,
    pulseScale: Float,
    isCompact: Boolean
) {
    val statusText =
        when (status) {
            GameStatus.BETTING -> stringResource(Res.string.status_betting)
            GameStatus.IDLE -> stringResource(Res.string.status_idle)
            GameStatus.PLAYING -> stringResource(Res.string.status_playing)
            GameStatus.DEALER_TURN -> stringResource(Res.string.status_dealer_turn)
            GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            GameStatus.PUSH -> stringResource(Res.string.status_push)
            else -> ""
        }
    Text(
        text = statusText,
        style =
            if (isCompact) {
                MaterialTheme.typography.headlineMedium
            } else {
                MaterialTheme.typography.headlineLarge
            },
        color = ModernGold,
        fontWeight = FontWeight.Black,
        modifier =
            Modifier.graphicsLayer {
                scaleX = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
                scaleY = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
            }
    )
}

@Composable
fun GameActions(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        if (state.status == GameStatus.PLAYING) {
            CasinoButton(
                text = stringResource(Res.string.hit),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.DEAL)
                    component.onAction(GameAction.Hit)
                },
                modifier = Modifier.weight(1f)
            )
            CasinoButton(
                text = stringResource(Res.string.stand),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.Stand)
                },
                modifier = Modifier.weight(1f)
            )

            val canSplit = state.canSplit()
            if (canSplit) {
                CasinoButton(
                    text = stringResource(Res.string.split),
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.DEAL)
                        component.onAction(GameAction.Split)
                    },
                    modifier = Modifier.weight(1f),
                    isStrategic = true
                )
            }

            val canDouble = state.canDoubleDown()
            if (canDouble) {
                CasinoButton(
                    text = "x2",
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.DEAL)
                        component.onAction(GameAction.DoubleDown)
                    },
                    modifier = Modifier.weight(0.6f),
                    isStrategic = true
                )
            }
        } else if (state.status != GameStatus.INSURANCE_OFFERED) {
            CasinoButton(
                text = stringResource(Res.string.deal),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.NewGame())
                },
                modifier = if (isCompact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth(0.8f)
            )
        }
    }
}

@Composable
fun HandRow(hand: Hand) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-40).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.cards.forEach { card ->
            key(card) {
                PlayingCard(
                    card = card,
                    isFaceUp = !card.isFaceDown
                )
            }
        }
    }
}
