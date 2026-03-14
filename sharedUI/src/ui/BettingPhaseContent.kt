package io.github.smithjustinn.blackjack.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.ChipSelector
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet
import sharedui.generated.resources.deal
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.status_betting

private fun formatCurrency(amount: Int): String {
    return "$$amount"
}

@Composable
fun BettingPhaseContent(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    require(state.status == GameStatus.BETTING)
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.status_betting),
            style =
                if (isCompact) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.headlineLarge
                },
            color = PrimaryGold,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "${stringResource(Res.string.balance)}: ${formatCurrency(state.balance)}",
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryGold
        )
        Spacer(modifier = Modifier.height(8.dp))
        AnimatedContent(
            targetState = state.currentBet,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "betAmount"
        ) { bet ->
            Text(
                text = "${stringResource(Res.string.bet)}: ${formatCurrency(bet)}",
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryGold
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        ChipSelector(
            balance = state.balance,
            onBetClick = { amount ->
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.PlaceBet(amount))
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            CasinoButton(
                text = stringResource(Res.string.reset_bet),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.ResetBet)
                },
                modifier = Modifier.weight(1f)
            )
            CasinoButton(
                text = stringResource(Res.string.deal),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.Deal)
                },
                modifier = Modifier.weight(1f),
                enabled = state.currentBet > 0
            )
        }
    }
}
