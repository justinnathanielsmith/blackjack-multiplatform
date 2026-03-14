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
import io.github.smithjustinn.blackjack.ui.theme.ChipBlue
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.ChipPurple
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft

private val CHIP_VALUES = listOf(1, 5, 10, 25, 100)

private fun chipColor(value: Int) =
    when (value) {
        1 -> WhiteSoft
        5 -> PokerRed
        10 -> ChipBlue
        25 -> ChipGreen
        50 -> PrimaryGold
        100 -> PokerBlack
        500 -> ChipPurple
        else -> PokerBlack
    }

private fun chipTextColor(value: Int) =
    when (value) {
        1, 50 -> FeltDark
        else -> WhiteSoft
    }

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
                chipColor = chipColor(value),
                textColor = chipTextColor(value),
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
