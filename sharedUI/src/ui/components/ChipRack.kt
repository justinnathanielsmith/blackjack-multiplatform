package io.github.smithjustinn.blackjack.ui.components

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.ui.theme.TableWoodDeep
import io.github.smithjustinn.blackjack.ui.theme.TableWoodEdge
import io.github.smithjustinn.blackjack.ui.theme.TableWoodRim
import io.github.smithjustinn.blackjack.ui.theme.TrayDarkBottom
import io.github.smithjustinn.blackjack.ui.theme.TrayDarkTop
import io.github.smithjustinn.blackjack.utils.DragTarget

private val CHIP_VALUES = listOf(1, 5, 10, 25, 100)

private fun breakdownBalance(
    balance: Int,
    chipValues: List<Int>
): Map<Int, Int> {
    var remaining = balance
    val breakdown = mutableMapOf<Int, Int>()
    for (value in chipValues.sortedDescending()) {
        val count = remaining / value
        if (count > 0) {
            breakdown[value] = count
        }
        remaining %= value
    }
    return breakdown
}

@Composable
fun ChipRack(
    balance: Int,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val breakdown = remember(balance) { breakdownBalance(balance, CHIP_VALUES) }

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
                CHIP_VALUES.forEach { value ->
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
                                            it.clip(CircleShape)
                                        } else {
                                            it
                                        }
                                    }

                            if (isTopChip) {
                                DragTarget(
                                    item = value,
                                    enabled = enabled,
                                    modifier = chipModifier
                                ) {
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
                                    )
                                }
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
