package io.github.smithjustinn.blackjack.ui.components.chips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.logic.ChipLogic
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.ui.theme.TableWoodDeep
import io.github.smithjustinn.blackjack.ui.theme.TableWoodEdge
import io.github.smithjustinn.blackjack.ui.theme.TableWoodRim
import io.github.smithjustinn.blackjack.ui.theme.TrayDarkBottom
import io.github.smithjustinn.blackjack.ui.theme.TrayDarkTop

/**
 * A wooden tray component that displays available chips for betting.
 *
 * This component handles the visual organization of chips into physical stacks
 * based on the player's [balance]. It provides an interactive interface for
 * selecting betting amounts and reporting the logical center of each chip
 * stack to the global animation registry.
 *
 * **Functional Intent:**
 * - **Balance Gates**: Denominations higher than the current [balance] appear
 *   as semi-transparent "ghosts" and are disabled.
 * - **Animated Fly-Outs**: Reports the layout position of the top-most chip in
 *   each stack via [onChipPositioned] to coordinate "flight" animations when
 *   chips are added to a bet.
 * - **Aesthetic Depth**: Uses a multi-layered design with shadows, borders,
 *   and gradients to match the premium wooden casino table theme.
 *
 * @param balance The player's total liquidity. Used to gate chip availability.
 * @param selectedAmount The current denomination selected for placing bets.
 * @param onChipSelected Callback invoked when a valid (enabled) chip stack is tapped.
 * @param modifier [Modifier] applied to the outer wooden frame of the tray.
 * @param onChipPositioned Callback triggered when a chip stack's center point
 *   is calculated. Receives the stack's chip value and its [Offset] in the root
 *   window coordinate space.
 */
@Composable
fun ChipRack(
    balance: Int,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onChipPositioned: (Int, Offset) -> Unit = { _, _ -> },
) {
    val breakdown = remember(balance) { ChipLogic.breakdownBalance(balance) }

    // Outer wooden tray frame
    Box(
        modifier =
            modifier
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(OakMedium, TableWoodDeep)
                        ),
                    shape = RoundedCornerShape(12.dp)
                ).border(
                    width = 2.dp,
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(TableWoodRim, TableWoodEdge)
                        ),
                    shape = RoundedCornerShape(12.dp)
                ).padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Inner tray area where chips rest
        Box(
            modifier =
                Modifier
                    .background(
                        brush =
                            Brush.verticalGradient(
                                colors = listOf(TrayDarkTop, TrayDarkBottom)
                            ),
                        shape = RoundedCornerShape(8.dp)
                    ).border(
                        width = 1.dp,
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BlackjackConfig.RACK_DENOMINATIONS.forEach { value ->
                    val enabled = balance >= value
                    val count = breakdown[value] ?: 0
                    val displayCount = if (count == 0) 1 else minOf(count, 5)

                    Box(
                        modifier = Modifier.height(72.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        for (i in 0 until displayCount) {
                            val isTopChip = (i == displayCount - 1)
                            val isGhost = (count == 0)

                            val chipModifier =
                                Modifier
                                    .padding(bottom = (i * 4).dp)
                                    .let { if (isGhost) it.alpha(0.3f) else it }
                                    .let {
                                        if (isTopChip) {
                                            it
                                                .clip(CircleShape)
                                                .onGloballyPositioned { coordinates ->
                                                    val center =
                                                        coordinates.positionInRoot() +
                                                            Offset(
                                                                x = coordinates.size.width / 2f,
                                                                y = coordinates.size.height / 2f,
                                                            )
                                                    onChipPositioned(value, center)
                                                }
                                        } else {
                                            it.clearAndSetSemantics {} // Decorative — suppress from a11y tree
                                        }
                                    }

                            if (isTopChip) {
                                BetChip(
                                    amount = value,
                                    chipColor = ChipUtils.chipColor(value),
                                    textColor = ChipUtils.chipTextColor(value),
                                    isActive = (value == selectedAmount),
                                    onClick = {
                                        if (enabled) {
                                            onChipSelected(value)
                                        }
                                    },
                                    enabled = enabled,
                                    modifier = chipModifier
                                )
                            } else {
                                BetChip(
                                    amount = value,
                                    chipColor = ChipUtils.chipColor(value),
                                    textColor = ChipUtils.chipTextColor(value),
                                    isActive = false,
                                    onClick = null,
                                    enabled = enabled,
                                    modifier = chipModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ChipRackPreview() {
    BlackjackTheme {
        ChipRack(
            balance = 1000,
            selectedAmount = 10,
            onChipSelected = {}
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ChipRackLimitedBalancePreview() {
    BlackjackTheme {
        ChipRack(
            balance = 20,
            selectedAmount = 5,
            onChipSelected = {}
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ChipRackSelectedPreview() {
    BlackjackTheme {
        ChipRack(
            balance = 500,
            selectedAmount = 100,
            onChipSelected = {}
        )
    }
}
