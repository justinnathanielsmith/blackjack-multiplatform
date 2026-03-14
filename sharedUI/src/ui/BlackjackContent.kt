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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltGreenDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreenLight
import io.github.smithjustinn.blackjack.ui.theme.ModernGold
import kotlinx.coroutines.launch

@Composable
fun BlackjackContent(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val audioService = LocalAppGraph.current.audioService
    val shakeOffset = remember { Animatable(0f) }

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
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(FeltGreenLight, FeltGreenDark)
                        )
                    ).offset(x = shakeOffset.value.dp)
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
            ) {
                if (state.status == GameStatus.PLAYER_WON) {
                    ConfettiEffect()
                }
                val dealerScore =
                    if (state.status == GameStatus.PLAYING && state.dealerHand.cards.size >= 2) {
                        "?"
                    } else {
                        state.dealerHand.score.toString()
                    }

                Column(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(32.dp))

                    // Dealer Area
                    Text(
                        "DEALER",
                        color = ModernGold.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Score: $dealerScore",
                        color = Color.White,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    HandRow(state.dealerHand, hideHoleCard = state.status == GameStatus.PLAYING)

                    Spacer(modifier = Modifier.weight(1f))

                    // Status Message
                    if (state.status != GameStatus.PLAYING) {
                        Text(
                            text = state.status.toString().uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = ModernGold,
                            fontWeight = FontWeight.Black,
                            modifier =
                                Modifier.graphicsLayer {
                                    scaleX = if (state.status == GameStatus.PUSH) pulseScale else 1f
                                    scaleY = if (state.status == GameStatus.PUSH) pulseScale else 1f
                                }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Player Area
                    HandRow(state.playerHand, hideHoleCard = false)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "PLAYER",
                        color = ModernGold.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "Score: ${state.playerHand.score}",
                        color = Color.White,
                        fontSize = 18.sp
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                    ) {
                        if (state.status == GameStatus.PLAYING) {
                            CasinoButton(
                                text = "Hit",
                                onClick = {
                                    audioService.playEffect(AudioService.SoundEffect.DEAL)
                                    component.onAction(GameAction.Hit)
                                },
                                modifier = Modifier.weight(1f)
                            )
                            CasinoButton(
                                text = "Stand",
                                onClick = {
                                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                                    component.onAction(GameAction.Stand)
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            CasinoButton(
                                text = "New Game",
                                onClick = {
                                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                                    component.onAction(GameAction.NewGame)
                                },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HandRow(
    hand: Hand,
    hideHoleCard: Boolean = false
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-40).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.cards.forEachIndexed { index, card ->
            val isFaceUp = !(hideHoleCard && index == 1)
            PlayingCard(
                card = card,
                isFaceUp = isFaceUp
            )
        }
    }
}
