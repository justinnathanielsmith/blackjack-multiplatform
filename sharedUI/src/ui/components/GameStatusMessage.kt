package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_dealing
import sharedui.generated.resources.status_idle
import sharedui.generated.resources.status_player_won
import sharedui.generated.resources.status_playing
import sharedui.generated.resources.status_push
import sharedui.generated.resources.net_result_won
import sharedui.generated.resources.net_result_lost
import sharedui.generated.resources.net_result_push

@Composable
fun GameStatusMessage(
    status: GameStatus,
    netPayout: Int? = null,
    isCompact: Boolean = false,
    isBlackjack: Boolean = false,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by
        infiniteTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = if (isBlackjack) 1.15f else 1.04f,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(if (isBlackjack) 400 else 600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        )

    val shimmerX = remember { Animatable(-0.5f) }
    LaunchedEffect(status, isBlackjack) {
        if (status == GameStatus.PLAYER_WON || status == GameStatus.PUSH || isBlackjack) {
            while (true) {
                shimmerX.animateTo(
                    targetValue = 1.5f,
                    animationSpec = tween(if (isBlackjack) 800 else 1200, easing = LinearEasing)
                )
                shimmerX.snapTo(-0.5f)
                kotlinx.coroutines.delay(if (isBlackjack) 200 else 400)
            }
        }
    }

    val ring1Radius = remember { Animatable(0f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Radius = remember { Animatable(0f) }
    val ring2Alpha = remember { Animatable(0f) }
    val ring3Radius = remember { Animatable(0f) }
    val ring3Alpha = remember { Animatable(0f) }

    LaunchedEffect(status, isBlackjack) {
        if (status == GameStatus.PLAYER_WON) {
            ring1Radius.snapTo(0f)
            ring1Alpha.snapTo(1f)
            ring2Radius.snapTo(0f)
            ring2Alpha.snapTo(1f)
            ring3Radius.snapTo(0f)
            ring3Alpha.snapTo(1f)
            launch {
                launch { ring1Radius.animateTo(1f, tween(600)) }
                ring1Alpha.animateTo(0f, tween(600))
            }
            launch {
                kotlinx.coroutines.delay(250)
                launch { ring2Radius.animateTo(1f, tween(600)) }
                ring2Alpha.animateTo(0f, tween(600))
            }
            launch {
                kotlinx.coroutines.delay(500)
                launch { ring3Radius.animateTo(1f, tween(600)) }
                ring3Alpha.animateTo(0f, tween(600))
            }
        }
    }

    val statusText =
        when {
            isBlackjack -> "Blackjack!"
            status == GameStatus.BETTING -> stringResource(Res.string.status_betting)
            status == GameStatus.IDLE -> stringResource(Res.string.status_idle)
            status == GameStatus.DEALING -> stringResource(Res.string.status_dealing)
            status == GameStatus.PLAYING -> stringResource(Res.string.status_playing)
            status == GameStatus.DEALER_TURN -> stringResource(Res.string.status_dealer_turn)
            status == GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            status == GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            status == GameStatus.PUSH -> stringResource(Res.string.status_push)
            else -> ""
        }

    val accentColor =
        when (status) {
            GameStatus.PLAYER_WON -> PrimaryGold
            GameStatus.DEALER_WON -> TacticalRed
            GameStatus.PUSH -> Color.White
            else -> PrimaryGold.copy(alpha = 0.8f)
        }

    Box(
        modifier =
            Modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }.drawWithCache {
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent),
                            radius = size.maxDimension * 0.8f
                        )
                    onDrawBehind {
                        drawCircle(
                            color = accentColor.copy(alpha = ring1Alpha.value * 0.55f),
                            radius = ring1Radius.value * size.maxDimension * 0.7f,
                            style = Stroke(2.dp.toPx()),
                        )
                        drawCircle(
                            color = accentColor.copy(alpha = ring2Alpha.value * 0.45f),
                            radius = ring2Radius.value * size.maxDimension * 0.7f,
                            style = Stroke(1.5.dp.toPx()),
                        )
                        drawCircle(
                            color = accentColor.copy(alpha = ring3Alpha.value * 0.35f),
                            radius = ring3Radius.value * size.maxDimension * 0.7f,
                            style = Stroke(1.dp.toPx()),
                        )
                        drawRoundRect(
                            brush = glowBrush,
                            cornerRadius = CornerRadius(32.dp.toPx(), 32.dp.toPx())
                        )
                    }
                }.clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(GlassDark, Color.Black.copy(alpha = 0.9f))
                    )
                ).border(
                    width = 2.dp,
                    brush =
                        Brush.horizontalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.1f), accentColor, accentColor.copy(alpha = 0.1f))
                        ),
                    shape = RoundedCornerShape(32.dp),
                ).padding(horizontal = 48.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        val isTerminal = status == GameStatus.PLAYER_WON ||
            status == GameStatus.DEALER_WON ||
            status == GameStatus.PUSH

        val netLabel: String? =
            if (isTerminal && netPayout != null) {
                when {
                    netPayout > 0 -> stringResource(Res.string.net_result_won, netPayout)
                    netPayout < 0 -> stringResource(Res.string.net_result_lost, -netPayout)
                    else -> stringResource(Res.string.net_result_push)
                }
            } else null

        val netLabelColor =
            when {
                netPayout != null && netPayout > 0 -> PrimaryGold
                netPayout != null && netPayout < 0 -> TacticalRed
                else -> Color.White.copy(alpha = 0.7f)
            }

        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusText.uppercase(),
                style =
                    if (isCompact) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.displayMedium
                    }.copy(letterSpacing = 4.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.drawWithCache {
                        val shimmerBrush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        accentColor.copy(alpha = 0.3f),
                                        accentColor,
                                        accentColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                start = Offset(size.width * shimmerX.value, 0f),
                                end = Offset(size.width * (shimmerX.value + 0.3f), size.height)
                            )
                        onDrawWithContent {
                            drawContent()
                            if (status == GameStatus.PLAYER_WON || status == GameStatus.PUSH) {
                                drawRect(
                                    brush = shimmerBrush,
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                    },
            )
            if (netLabel != null) {
                Text(
                    text = netLabel,
                    color = netLabelColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 2.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
