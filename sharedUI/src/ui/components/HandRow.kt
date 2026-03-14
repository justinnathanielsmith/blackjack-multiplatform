package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Hand

@Composable
fun HandRow(hand: Hand) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-40).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hand.cards.forEach { card ->
            key(card) {
                PlayingCard(
                    card = card,
                    isFaceUp = !card.isFaceDown,
                )
            }
        }
    }
}
