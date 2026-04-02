package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.deal
import sharedui.generated.resources.deal_description
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.reset_bet_description

@Composable
fun BettingActions(
    canDeal: Boolean,
    canReset: Boolean,
    onReset: () -> Unit,
    onDeal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canDeal) 1.05f else 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "dealPulse"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        CasinoButton(
            text = stringResource(Res.string.reset_bet),
            onClick = onReset,
            enabled = canReset,
            contentDescription = stringResource(Res.string.reset_bet_description),
            modifier = Modifier.weight(1f),
            containerColor = GlassDark,
            contentColor = Color.White,
        )
        CasinoButton(
            text = stringResource(Res.string.deal),
            onClick = onDeal,
            modifier =
                Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
            enabled = canDeal,
            isStrategic = true,
            showShine = canDeal,
            contentDescription = stringResource(Res.string.deal_description),
            containerColor = PrimaryGold,
            contentColor = BackgroundDark,
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun BettingActionsPreview() {
    BettingActions(
        canDeal = true,
        canReset = true,
        onReset = {},
        onDeal = {}
    )
}
