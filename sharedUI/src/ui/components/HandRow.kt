package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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
    val overlapOffset = Dimensions.Card.OverlapOffsetRaw.dp * cardScale

    Layout(
        modifier = Modifier.animateContentSize(),
        content = {
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
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        val n = placeables.size
        val cardW = placeables.firstOrNull()?.width ?: 0
        val cardH = placeables.maxOfOrNull { it.height } ?: 0
        val stepPx = overlapOffset.roundToPx() + cardW

        val totalWidth = if (n == 0) 0 else cardW + (n - 1) * stepPx

        layout(totalWidth.coerceAtLeast(0), cardH) {
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(i * stepPx, 0)
            }
        }
    }
}
