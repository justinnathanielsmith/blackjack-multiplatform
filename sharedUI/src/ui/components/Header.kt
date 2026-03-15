package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
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
import androidx.compose.ui.graphics.Color
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
    onStrategyClick: () -> Unit = {}
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
            HeaderIcon("strategy", onClick = onStrategyClick)
            HeaderIcon("history")
            HeaderIcon("settings", onClick = onSettingsClick)
        }
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
            text = when (text) {
                "settings" -> "⚙️"
                "strategy" -> "💡"
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
