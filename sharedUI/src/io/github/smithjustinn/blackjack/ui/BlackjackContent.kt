package io.github.smithjustinn.blackjack.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.FeltGreenDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreenLight
import io.github.smithjustinn.blackjack.ui.theme.ModernGold
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.services.AudioService

@Composable
fun BlackjackContent(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val audioService = LocalAppGraph.current.audioService

    LaunchedEffect(state.status) {
        when (state.status) {
            GameStatus.PLAYER_WON -> audioService.playEffect(AudioService.SoundEffect.WIN)
            GameStatus.DEALER_WON -> audioService.playEffect(AudioService.SoundEffect.LOSE)
            else -> {}
        }
    }

    BlackjackTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(FeltGreenLight, FeltGreenDark)
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                val dealerScore = if (state.status == GameStatus.PLAYING && state.dealerHand.cards.isNotEmpty()) {
                    "?"
                } else {
                    state.dealerHand.score.toString()
                }

                Column(
                    modifier = Modifier
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
                    HandRow(state.dealerHand, hideFirstCard = state.status == GameStatus.PLAYING)

                    Spacer(modifier = Modifier.weight(1f))

                    // Status Message
                    if (state.status != GameStatus.PLAYING) {
                        Text(
                            text = state.status.toString().uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = ModernGold,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Player Area
                    HandRow(state.playerHand, hideFirstCard = false)
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
fun HandRow(hand: Hand, hideFirstCard: Boolean = false) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-40).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.cards.forEachIndexed { index, card ->
            val isFaceUp = !(hideFirstCard && index == 0)
            PlayingCard(
                card = card,
                isFaceUp = isFaceUp
            )
        }
    }
}
