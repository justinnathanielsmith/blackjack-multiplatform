package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.Dimensions

@Composable
fun HandRow(
    hand: Hand,
    isDealer: Boolean = false,
    isCompact: Boolean = false,
    isSlowReveal: Boolean = false,
    scale: Float? = null,
    isNearMiss: Boolean = false,
) {
    val cardScale = scale ?: if (isCompact) 0.8f else 1f
    val cardWidth = Dimensions.Card.StandardWidth * cardScale
    val overlapOffset = Dimensions.Card.OverlapOffsetRaw.dp * cardScale

    androidx.compose.foundation.layout.Row(
        modifier = Modifier.animateContentSize(),
        horizontalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(overlapOffset),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.cards.forEachIndexed { index, card ->
            key(card) {
                PlayingCard(
                    card = card,
                    isFaceUp = !card.isFaceDown,
                    isDealer = isDealer,
                    animationDelay = index * AnimationConstants.CardDealDelay,
                    animationDurationMs =
                        if (isSlowReveal && isDealer) {
                            AnimationConstants.CardRevealDurationSlow
                        } else {
                            AnimationConstants.CardRevealDurationDefault
                        },
                    scale = cardScale,
                    isNearMiss = isNearMiss
                )
            }
        }
    }
}
