package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.ChipSelector
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet
import sharedui.generated.resources.deal
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.status_betting

private fun formatCurrency(amount: Int): String = "$$amount"

@Composable
fun BettingPhaseScreen(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    require(state.status == GameStatus.BETTING)
    
    Column(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.status_betting).uppercase(),
            style = if (isCompact) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.headlineMedium,
            color = PrimaryGold,
            fontWeight = FontWeight.Black,
            letterSpacing = 4.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Glassmorphic Info Card
        Column(
            modifier = Modifier
                .fillMaxWidth(if (isCompact) 1f else 0.8f)
                .clip(RoundedCornerShape(24.dp))
                .background(GlassDark)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.balance).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
                letterSpacing = 2.sp
            )
            AnimatedContent(
                targetState = state.balance,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "balanceRoll",
            ) { balance ->
                Text(
                    text = formatCurrency(balance),
                    style = if (isCompact) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(Res.string.bet).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGold.copy(alpha = 0.8f),
                letterSpacing = 2.sp
            )
            AnimatedContent(
                targetState = state.currentBet,
                transitionSpec = {
                    fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "betAmount",
            ) { bet ->
                Text(
                    text = formatCurrency(bet),
                    style = if (isCompact) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Black
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        ChipSelector(
            balance = state.balance,
            onBetClick = { amount ->
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.PlaceBet(amount))
            },
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        ) {
            CasinoButton(
                text = stringResource(Res.string.reset_bet),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.ResetBet)
                },
                modifier = Modifier.weight(1f),
                containerColor = Color.Transparent,
                contentColor = Color.White.copy(alpha = 0.7f)
            )
            CasinoButton(
                text = stringResource(Res.string.deal),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.Deal)
                },
                modifier = Modifier.weight(1f),
                enabled = state.currentBet > 0,
                isStrategic = true
            )
        }
    }
}
