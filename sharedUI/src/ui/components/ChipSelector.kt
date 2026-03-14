package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.OakMedium
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed

private val CHIP_VALUES = listOf(10, 50, 100)

private fun chipColor(value: Int) =
    when (value) {
        10 -> PokerRed
        50 -> PrimaryGold
        100 -> PokerBlack
        else -> OakMedium
    }

@Composable
fun ChipSelector(
    balance: Int,
    onBetClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CHIP_VALUES.forEach { value ->
            val enabled = balance >= value
            CasinoButton(
                text = "$$value",
                onClick = { onBetClick(value) },
                modifier = Modifier.size(width = 72.dp, height = 72.dp),
                contentColor = PrimaryGold,
                containerColor = chipColor(value)
            )
        }
    }
}
