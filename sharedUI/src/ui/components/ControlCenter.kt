package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.formatWithCommas
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet_total_label
import sharedui.generated.resources.currency_template

@Composable
fun ControlCenter(
    state: GameState,
    component: BlackjackComponent,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    onResetBet: () -> Unit,
    onDeal: () -> Unit,
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

        // Actions for BETTING
        AnimatedVisibility(
            visible = state.status == GameStatus.BETTING,
            enter = fadeIn(tween(300)) + expandVertically(tween(300), expandFrom = Alignment.Top),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(300), shrinkTowards = Alignment.Top),
        ) {
            BettingActions(
                canDeal = state.currentBets.isNotEmpty() && state.currentBets.all { it > 0 },
                onReset = onResetBet,
                onDeal = onDeal,
                modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
            )
        }

        val isBetting = state.status == GameStatus.BETTING
        val chipRackTranslationY by animateDpAsState(
            targetValue = if (isBetting) 0.dp else 100.dp,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "chipRackTranslationY"
        )

        // Persistent Chip Rack
        ChipRack(
            balance = state.balance,
            selectedAmount = selectedAmount,
            onChipSelected = onChipSelected,
            modifier =
                Modifier
                    .padding(bottom = 16.dp)
                    .graphicsLayer {
                        translationY = chipRackTranslationY.toPx()
                    }
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
    val formattedAmount = stringResource(Res.string.currency_template, animatedAmount.formatWithCommas())

    Column(
        modifier =
            Modifier.semantics(mergeDescendants = true) {
                contentDescription = "$label: $formattedAmount"
            },
        horizontalAlignment = alignment,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = stringResource(Res.string.currency_template, animatedAmount.formatWithCommas()),
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryGold,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace // Monospaced for stability
        )
    }
}
