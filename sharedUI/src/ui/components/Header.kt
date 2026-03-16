package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
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
import sharedui.generated.resources.btn_auto_deal_description
import sharedui.generated.resources.btn_rules_description
import sharedui.generated.resources.btn_settings_description
import sharedui.generated.resources.btn_strategy_description
import kotlin.math.abs

@Composable
fun Header(
    balance: Int,
    isAutoDealEnabled: Boolean,
    onAutoDealToggle: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onStrategyClick: () -> Unit = {},
    onRulesClick: () -> Unit = {}
) {
    val previousBalance = remember { mutableIntStateOf(balance) }
    val jiggleX = remember { Animatable(0f) }

    LaunchedEffect(balance) {
        if (balance > previousBalance.intValue) {
            jiggleX.snapTo(0f)
            jiggleX.animateTo(-6f, tween(40))
            jiggleX.animateTo(6f, tween(60))
            jiggleX.animateTo(-4f, tween(50))
            jiggleX.animateTo(4f, tween(50))
            jiggleX.animateTo(0f, tween(40, easing = FastOutSlowInEasing))
        }
    }

    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec =
            tween(
                durationMillis = if (abs(balance - previousBalance.intValue) > 200) 600 else 300,
                easing = FastOutSlowInEasing,
            ),
        label = "balanceRoll",
        finishedListener = { previousBalance.intValue = it },
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier =
                Modifier
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Balance: $$formattedBalance"
                    }.graphicsLayer { translationX = jiggleX.value },
        ) {
            Text(
                text = stringResource(Res.string.balance).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f),
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
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AutoDealIcon(enabled = isAutoDealEnabled, onClick = onAutoDealToggle)
            HeaderIcon(
                "rules",
                contentDescription = stringResource(Res.string.btn_rules_description),
                onClick = onRulesClick
            )
            HeaderIcon(
                "strategy",
                contentDescription = stringResource(Res.string.btn_strategy_description),
                onClick = onStrategyClick
            )
            HeaderIcon(
                "settings",
                contentDescription = stringResource(Res.string.btn_settings_description),
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
internal fun AutoDealIcon(
    enabled: Boolean,
    onClick: () -> Unit = {}
) {
    val autoDealDescription = stringResource(Res.string.btn_auto_deal_description)
    val infiniteTransition = rememberInfiniteTransition(label = "autoDealPulse")

    // Breathing scale: 1.0 → 1.1 → 1.0
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 1.1f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathScale",
    )

    // Border alpha: wider range than before (0.3 → 1.0)
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (enabled) 1.0f else 0.3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(900, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "autoBorderAlpha",
    )

    // Sonar pulse rings
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 2.0f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseScale1",
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseAlpha1",
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 2.0f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseScale2",
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(2000, delayMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseAlpha2",
    )

    val borderColor = if (enabled) PrimaryGold.copy(alpha = borderAlpha) else GlassLight
    val backgroundColor = GlassDark

    Box(
        modifier =
            Modifier
                .size(32.dp)
                .semantics { contentDescription = autoDealDescription }
                .drawBehind {
                    if (enabled) {
                        drawCircle(
                            color = PrimaryGold.copy(alpha = pulseAlpha1),
                            radius = (size.minDimension / 2) * pulseScale1,
                        )
                        drawCircle(
                            color = PrimaryGold.copy(alpha = pulseAlpha2),
                            radius = (size.minDimension / 2) * pulseScale2,
                        )
                    }
                }.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(backgroundColor, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = "⚡", fontSize = 14.sp)
    }
}

@Composable
private fun HeaderIcon(
    text: String,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier =
            Modifier
                .size(32.dp)
                .semantics { this.contentDescription = contentDescription }
                .background(GlassDark, RoundedCornerShape(16.dp))
                .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
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
            fontSize = 14.sp,
            modifier = Modifier.alpha(0.9f)
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
