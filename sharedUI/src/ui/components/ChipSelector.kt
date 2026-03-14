package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft

private val CHIP_VALUES = listOf(10, 50, 100)

private fun chipColor(value: Int) =
    when (value) {
        10 -> PokerRed
        50 -> PrimaryGold
        100 -> PokerBlack
        else -> PokerBlack
    }

private fun chipTextColor(value: Int) =
    when (value) {
        50 -> FeltDark
        else -> WhiteSoft
    }

@Composable
fun ChipSelector(
    balance: Int,
    onBetClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CHIP_VALUES.forEach { value ->
            val enabled = balance >= value
            BetChip(
                amount = value,
                chipColor = chipColor(value),
                textColor = chipTextColor(value),
                onClick = { onBetClick(value) },
                enabled = enabled
            )
        }
    }
}
