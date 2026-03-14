package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Hand

@Composable
fun HandRow(
    hand: Hand,
    isDealer: Boolean = false,
    isCompact: Boolean = false,
) {
    val cardSpacing = if (isCompact) (-30).dp else (-40).dp
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
                )
            }
        }
    }
}
