package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import kotlin.math.abs

@Composable
fun Header(
    balance: Int,
    onSettingsClick: () -> Unit = {},
    onStrategyClick: () -> Unit = {},
    onRulesClick: () -> Unit = {}
) {
    var previousBalance by remember { mutableStateOf(balance) }

    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec =
            tween(
                durationMillis = if (abs(balance - previousBalance) > 200) 600 else 300,
                easing = FastOutSlowInEasing,
            ),
        label = "balanceRoll",
        finishedListener = { previousBalance = it },
    )

    val formattedBalance =
        remember(animatedBalance) {
            animatedBalance.formatWithCommas()
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GlassDark)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier.semantics(mergeDescendants = true) {
                    contentDescription = "Balance: $$formattedBalance"
                },
        ) {
            Text(
                text = stringResource(Res.string.balance).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
            Text(
                text = "$$formattedBalance",
                style = MaterialTheme.typography.headlineSmall,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderIcon("rules", onClick = onRulesClick)
            HeaderIcon("strategy", onClick = onStrategyClick)
            HeaderIcon("settings", onClick = onSettingsClick)
        }
    }
}

@Composable
internal fun AutoDealIcon(enabled: Boolean, onClick: () -> Unit = {}) {
    val infiniteTransition = rememberInfiniteTransition(label = "autoDealPulse")

    // Sonar ring progress: 0f → 1f, Restart so rings always expand outward
    val ringProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringProgress",
    )

    // Breathing scale: 1.0 → 1.1 → 1.0
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 1.1f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // Border alpha: wider range than before (0.3 → 1.0)
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (enabled) 1.0f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "autoBorderAlpha",
    )

    val borderColor = if (enabled) PrimaryGold.copy(alpha = borderAlpha) else GlassLight
    val backgroundColor = if (enabled) PrimaryGold.copy(alpha = 0.15f) else GlassDark

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                if (enabled) {
                    val baseRadius = size.minDimension / 2f
                    repeat(3) { i ->
                        val phase = (ringProgress + i / 3f) % 1f
                        val ringAlpha = (1f - phase) * 0.5f
                        val radius = baseRadius * (1f + phase * 1.5f)
                        drawCircle(
                            color = PrimaryGold,
                            radius = radius,
                            alpha = ringAlpha,
                            style = Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
            }
            .background(backgroundColor, RoundedCornerShape(20.dp))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "⚡", fontSize = 18.sp)
    }
}

@Composable
private fun HeaderIcon(
    text: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(GlassDark, RoundedCornerShape(20.dp))
                .border(1.dp, GlassLight, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                when (text) {
                    "settings" -> "⚙️"
                    "strategy" -> "💡"
                    "rules" -> "📜"
                    else -> "🕒"
                },
            fontSize = 18.sp,
        )
    }
}

private fun Int.formatWithCommas(): String {
    val s = this.toString()
    if (s.length <= 3) return s
    return s
        .reversed()
        .chunked(3)
        .joinToString(",")
        .reversed()
}
