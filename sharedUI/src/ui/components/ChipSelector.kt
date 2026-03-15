package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft
import io.github.smithjustinn.blackjack.ui.components.ChipUtils

private val CHIP_VALUES = listOf(1, 5, 10, 25, 100)



@Composable
fun ChipSelector(
    balance: Int,
    onBetClick: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val chipPositions = remember { mutableStateMapOf<Int, Offset>() }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CHIP_VALUES.forEach { value ->
            val enabled = balance >= value
            BetChip(
                amount = value,
                chipColor = ChipUtils.chipColor(value),
                textColor = ChipUtils.chipTextColor(value),
                onClick = {
                    val position = chipPositions[value] ?: Offset.Zero
                    onBetClick(value, position)
                },
                enabled = enabled,
                modifier =
                    Modifier.onGloballyPositioned {
                        val center = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        chipPositions[value] = center
                    },
            )
        }
    }
}
