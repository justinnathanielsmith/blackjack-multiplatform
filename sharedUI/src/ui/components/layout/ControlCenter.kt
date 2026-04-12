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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.action.GameAction
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

/**
 * The primary user interaction hub for the Blackjack game.
 *
 * This component hosts the context-aware action buttons (Hit, Stand, etc.), betting controls,
 * and the chip rack. It automatically transitions its layout based on the [isBetting] and
 * [isPlaying] flags, ensuring only relevant actions are presented to the player.
 *
 * It uses [AnimatedVisibility] and [animateContentSize] to provide smooth transitions
 * between betting and playing phases.
 *
 * @param isBetting True if the player is currently in the betting phase.
 * @param isPlaying True if the player is currently making decisions on a dealt hand.
 * @param totalBet The current cumulative bet on the table across all positions.
 * @param balance The player's current bankroll (displayed in the footer status bar).
 * @param canDeal Eligibility flag for the DEAL action (must have non-zero bets).
 * @param canResetBet Eligibility flag for clearing current bets.
 * @param canSplit Eligibility flag for splitting the active hand.
 * @param canDoubleDown Eligibility flag for doubling down on the active hand.
 * @param canSurrender Eligibility flag for surrendering the active hand.
 * @param activeHandTension A 0f-1f value representing the "intensity" of the current hand
 *        (e.g., close to 21 or potentially busting), used for UI effects.
 * @param component The [BlackjackComponent] providing audio and domain action dispatch.
 * @param selectedAmount The value of the currently selected chip (e.g., 5, 25, 100).
 * @param onChipSelected Callback when a different chip value is picked.
 * @param onResetBet Callback when the player clears their table bets.
 * @param onDeal Callback when the player initiates the dealing phase.
 * @param modifier [Modifier] applied to the root [Column] of the control center.
 * @param isCompact If true, uses a tighter layout optimized for multi-hand play or small screens.
 * @param onChipPositioned Callback emitting the global [Offset] of each chip in the rack,
 *        used to anchor fly-to-bet animations in the overlay layer.
 */
@Composable
fun ControlCenter(
    isBetting: Boolean,
    isPlaying: Boolean,
    totalBet: Int,
    balance: Int,
    canDeal: Boolean,
    canResetBet: Boolean,
    canSplit: Boolean,
    canDoubleDown: Boolean,
    canSurrender: Boolean,
    activeHandTension: Float,
    component: BlackjackComponent,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    onResetBet: () -> Unit,
    onDeal: () -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false,
    onChipPositioned: (Int, Offset) -> Unit = { _, _ -> },
) {
    // Audio+action wiring lives here — GameActions is a pure UI component with no component coupling
    val onHit =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Hit)
            }
        }
    val onStand =
        remember(component) {
            {
                component.onPlayClick()
                component.onAction(GameAction.Stand)
            }
        }
    val onDoubleDown =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.DoubleDown)
            }
        }
    val onSplit =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Split)
            }
        }
    val onSurrender =
        remember(component) {
            {
                component.onPlayClick()
                component.onAction(GameAction.Surrender)
            }
        }

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Floating Action Buttons
        GameActions(
            isPlaying = isPlaying,
            canSplit = canSplit,
            canDoubleDown = canDoubleDown,
            canSurrender = canSurrender,
            activeHandTension = activeHandTension,
            onHit = onHit,
            onStand = onStand,
            onDoubleDown = onDoubleDown,
            onSplit = onSplit,
            onSurrender = onSurrender,
            isCompact = isCompact
        )

        val canReset = canResetBet

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
                    canDeal = canDeal,
                    canReset = canReset,
                    onReset = onResetBet,
                    onDeal = onDeal,
                    modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                )

                // Chip Rack
                ChipRack(
                    balance = balance,
                    selectedAmount = selectedAmount,
                    onChipSelected = onChipSelected,
                    onChipPositioned = onChipPositioned,
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
            TotalBetPill(amount = totalBet)
        }
    }
}

@Composable
private fun TotalBetPill(
    amount: Int,
    modifier: Modifier = Modifier
) {
    // Bolt Performance Optimization: Narrow recomposition scope to skip static decorations
    // during animation. Static labels and the outer Box only recompose when the target 'amount' changes.

    // Bolt: Use target strings for semantics to stabilize announcements.
    val label = stringResource(Res.string.bet_total_label)
    val currencyTemplate = stringResource(Res.string.currency_template)
    val financialDataDescTemplate = stringResource(Res.string.financial_data_content_description)

    val targetFormattedAmount =
        remember(amount) {
            currencyTemplate.replace("%1\$s", amount.formatWithCommas())
        }

    val accessibilityDescription =
        remember(targetFormattedAmount) {
            financialDataDescTemplate
                .replace("%1\$s", label)
                .replace("%2\$s", targetFormattedAmount)
        }

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
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            // Scoped animated text component
            AnimatedAmountDisplay(
                amount = amount,
                currencyTemplate = currencyTemplate
            )
        }
    }
}

@Composable
private fun AnimatedAmountDisplay(
    amount: Int,
    currencyTemplate: String
) {
    val animatedAmount by animateIntAsState(
        targetValue = amount,
        animationSpec = tween(durationMillis = 500)
    )

    // Bolt: Formatting inside remember ensures we only recompute string when animated amount jumps.
    val formatted =
        remember(animatedAmount) {
            currencyTemplate.replace("%1\$s", animatedAmount.formatWithCommas())
        }

    Text(
        text = formatted,
        style = MaterialTheme.typography.titleMedium,
        color = PrimaryGold,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Monospace
    )
}
