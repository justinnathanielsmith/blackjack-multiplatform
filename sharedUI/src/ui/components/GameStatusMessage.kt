package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.status_dealing
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_idle
import sharedui.generated.resources.status_player_won
import sharedui.generated.resources.status_playing
import sharedui.generated.resources.status_push

@Composable
fun GameStatusMessage(
    status: GameStatus,
    isCompact: Boolean = false,
) {
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

    val statusText =
        when (status) {
            GameStatus.BETTING -> stringResource(Res.string.status_betting)
            GameStatus.IDLE -> stringResource(Res.string.status_idle)
            GameStatus.DEALING -> stringResource(Res.string.status_dealing)
            GameStatus.PLAYING -> stringResource(Res.string.status_playing)
            GameStatus.DEALER_TURN -> stringResource(Res.string.status_dealer_turn)
            GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            GameStatus.PUSH -> stringResource(Res.string.status_push)
            else -> ""
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(GlassDark)
                .border(
                    1.dp,
                    when (status) {
                        GameStatus.PLAYER_WON -> PrimaryGold.copy(alpha = 0.8f)
                        GameStatus.DEALER_WON -> Color.Red.copy(alpha = 0.6f)
                        else -> Color.White.copy(alpha = 0.2f)
                    },
                    RoundedCornerShape(24.dp),
                ).padding(horizontal = 32.dp, vertical = 16.dp),
    ) {
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
}
