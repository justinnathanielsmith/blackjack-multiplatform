package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_idle
import sharedui.generated.resources.status_player_won
import sharedui.generated.resources.status_playing
import sharedui.generated.resources.status_push

@Composable
fun GameStatusMessage(
    status: GameStatus,
    pulseScale: Float,
    isCompact: Boolean,
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
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.displayMedium
            },
        color = PrimaryGold,
        fontWeight = FontWeight.Black,
        modifier =
            Modifier.graphicsLayer {
                scaleX = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
                scaleY = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
            },
    )
}
