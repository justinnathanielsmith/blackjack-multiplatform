package io.github.smithjustinn.blackjack.ui.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.components.actions.BettingActions
import io.github.smithjustinn.blackjack.ui.components.actions.GameActions
import io.github.smithjustinn.blackjack.ui.components.chips.ChipRack
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.utils.formatWithCommas
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet_total_label
import sharedui.generated.resources.currency_template
import sharedui.generated.resources.financial_data_content_description

@Composable
fun ControlCenter(
    state: GameState,
    component: BlackjackComponent,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    onResetBet: () -> Unit,
    onDeal: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Action Buttons
        GameActions(
            state = state,
            component = component,
            isCompact = isCompact
        )

        val isBetting = state.status == GameStatus.BETTING
        // Domain predicates: betting eligibility belongs in GameState, not the UI layer
        val canReset = state.canResetBet

        // Actions for BETTING
        AnimatedVisibility(
            visible = isBetting,
            enter =
                fadeIn(tween(AnimationConstants.ActionPlayingSlideDuration)) +
                    expandVertically(tween(AnimationConstants.ActionPlayingSlideDuration), expandFrom = Alignment.Top),
            exit =
                fadeOut(tween(AnimationConstants.ActionPlayingSlideDuration)) +
                    shrinkVertically(
                        tween(AnimationConstants.ActionPlayingSlideDuration),
                        shrinkTowards = Alignment.Top
                    ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BettingActions(
                    canDeal = state.canDeal,
                    canReset = canReset,
                    onReset = onResetBet,
                    onDeal = onDeal,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )

                // Chip Rack
                ChipRack(
                    balance = state.balance,
                    selectedAmount = selectedAmount,
                    onChipSelected = onChipSelected,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        // Footer Bar - Floating Style
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(safeDrawingInsets().only(WindowInsetsSides.Bottom))
                    .padding(bottom = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            TotalBetPill(amount = state.totalBet)
        }
    }
}

@Composable
private fun TotalBetPill(
    amount: Int,
    modifier: Modifier = Modifier
) {
    val animatedAmount by animateIntAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 500)
    )
    val formattedAmount = stringResource(Res.string.currency_template, animatedAmount.formatWithCommas())

    val accessibilityDescription =
        stringResource(
            Res.string.financial_data_content_description,
            stringResource(Res.string.bet_total_label),
            formattedAmount
        )

    Box(
        modifier =
            modifier
                .semantics(mergeDescendants = true) {
                    contentDescription = accessibilityDescription
                    liveRegion = LiveRegionMode.Polite
                }.background(GlassDark, RoundedCornerShape(percent = 50))
                .border(1.dp, PrimaryGold.copy(alpha = 0.4f), RoundedCornerShape(percent = 50))
                .padding(horizontal = 20.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.bet_total_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
