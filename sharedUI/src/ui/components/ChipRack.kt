package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.utils.DragTarget
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape

private val CHIP_VALUES = listOf(1, 5, 10, 25, 100)

@Composable
fun ChipRack(
    balance: Int,
    selectedAmount: Int,
    onChipSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Outer wooden tray frame
    Box(
        modifier =
            modifier
                .shadow(12.dp, RoundedCornerShape(12.dp))
                .background(
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(OakMedium, Color(0xFF2A1C12))
                        ),
                    shape = RoundedCornerShape(12.dp)
                ).border(
                    width = 2.dp,
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF5A3A22), Color(0xFF1A100A))
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
                                colors = listOf(Color(0xFF111111), Color(0xFF222222))
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                CHIP_VALUES.forEach { value ->
                    val enabled = balance >= value
                    DragTarget(
                        item = value,
                        modifier = Modifier.clip(CircleShape)
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
                }
            }
        }
    }
}

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
