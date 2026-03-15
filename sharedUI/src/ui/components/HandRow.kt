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
    isSlowReveal: Boolean = false,
    scale: Float? = null,
) {
    val cardScale = scale ?: if (isCompact) 0.8f else 1f
    val cardSpacing = (-40f * cardScale).dp
    Row(
        modifier = androidx.compose.ui.Modifier,
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
                    animationDurationMs = if (isSlowReveal && isDealer) 900 else 300,
                    scale = cardScale,
                )
            }
        }
    }
}
