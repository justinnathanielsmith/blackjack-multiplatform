package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.ui.graphics.Color
import io.github.smithjustinn.blackjack.ui.theme.ChipBlue
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.ChipPurple
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft

object ChipUtils {
    fun chipColor(value: Int): Color =
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

    fun chipTextColor(value: Int): Color =
        when (value) {
            1, 50 -> FeltDark
            else -> WhiteSoft
        }
}
