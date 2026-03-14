package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.ui.screens.LayoutMode

@Composable
fun HandRow(
    hand: Hand,
    isDealer: Boolean = false,
    layoutMode: LayoutMode = LayoutMode.PORTRAIT,
    scale: Float? = null,
) {
    val isCompact = layoutMode == LayoutMode.LANDSCAPE_COMPACT
    val cardScale = scale ?: if (isCompact) 0.8f else 1f
    val cardSpacing = when {
        cardScale < 0.7f -> (-22).dp
        isCompact -> (-35).dp
        else -> (-40).dp
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(cardSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hand.cards.forEachIndexed { index, card ->
            key(card) {
                PlayingCard(
                    card = card,
                    isFaceUp = !card.isFaceDown,
                    isDealer = isDealer,
                    animationDelay = index * 100,
                    scale = cardScale,
                )
            }
        }
    }
}
