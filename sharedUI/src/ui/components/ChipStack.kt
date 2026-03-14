package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.ChipBlue
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.ChipPurple
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft

private val DENOMINATIONS = listOf(500, 100, 50, 25, 10, 5, 1)

private fun getChipColor(value: Int): Color =
    when (value) {
        500 -> ChipPurple
        100 -> PokerBlack
        50 -> PrimaryGold
        25 -> ChipGreen
        10 -> ChipBlue
        5 -> PokerRed
        1 -> WhiteSoft
        else -> PokerBlack
    }

private fun getChipTextColor(value: Int): Color =
    when (value) {
        50, 1 -> FeltDark
        else -> WhiteSoft
    }

@Composable
fun ChipStack(
    amount: Int,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val chips =
        remember(amount) {
            val list = mutableListOf<Int>()
            var remaining = amount
            for (denom in DENOMINATIONS) {
                while (remaining >= denom && list.size < 5) { // Max 5 chips in visual stack
                    list.add(denom)
                    remaining -= denom
                }
            }
            // If we still have remaining, the last chip represents the rest (or just add one more if space)
            if (remaining > 0 && list.isNotEmpty()) {
                // Just use the last one to represent the total? No, let's just show the denominations.
                // If total is huge, we might need a better way, but for now 5 chips is a good visual.
            }
            list.reversed() // Bottom to top
        }

    if (chips.isEmpty() && amount > 0) {
        // Fallback for very small amounts if 1 is not in denom or similar
        BetChip(amount = amount, isActive = isActive, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
            chips.forEachIndexed { index, denom ->
                val yOffset = (-index * 4).dp
                val xOffset = (if (index % 2 == 0) 1 else -1).dp * (index * 0.5f)

                BetChip(
                    amount = if (index == chips.lastIndex) amount else denom,
                    chipColor = getChipColor(denom),
                    textColor = getChipTextColor(denom),
                    isActive = isActive,
                    modifier = Modifier.offset(x = xOffset, y = yOffset)
                )
            }
        }
    }
}
