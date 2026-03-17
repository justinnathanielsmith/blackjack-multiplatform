package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet_total_label

@Composable
fun ControlCenter(
    state: GameState,
    component: BlackjackComponent,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Action Buttons
        GameActions(
            state = state,
            component = component,
            isCompact = isCompact
        )

        // Footer Bar
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(GlassDark)
                    .windowInsetsPadding(safeDrawingInsets().only(WindowInsetsSides.Bottom))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Total Bet (Bottom Left)
            FinancialData(
                label = stringResource(Res.string.bet_total_label),
                amount = state.totalBet,
                alignment = Alignment.Start
            )

            // Balance (Bottom Right)
            FinancialData(
                label = stringResource(Res.string.balance),
                amount = state.balance,
                alignment = Alignment.End
            )
        }
    }
}

@Composable
private fun FinancialData(
    label: String,
    amount: Int,
    alignment: Alignment.Horizontal
) {
    val animatedAmount by animateIntAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 500)
    )

    Column(horizontalAlignment = alignment) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "$${animatedAmount.formatWithCommas()}",
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryGold,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace // Monospaced for stability
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
