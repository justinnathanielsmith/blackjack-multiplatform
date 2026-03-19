package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    isActive: Boolean = false,
) {
    val cardScale = scale ?: if (isCompact) 0.8f else 1f
    val overlapOffset = Dimensions.Card.OverlapOffsetRaw.dp * cardScale

    val verticalStep = 8.dp * cardScale

    Layout(
        modifier = Modifier,
        content = {
            val nCards = hand.cards.size
            val centerIndex = (nCards - 1) / 2f

            hand.cards.forEachIndexed { index, card ->
                key(card) {
                    // Curved Fanning
                    val distanceFromCenter = index - centerIndex
                    val fanAngle = distanceFromCenter * 3f // 3 degrees spread per card
                    val fanVerticalOffset = kotlin.math.abs(distanceFromCenter) * 2f // Slight dip on edges

                    Box(
                        modifier =
                            Modifier.graphicsLayer {
                                rotationZ = fanAngle
                                translationY = fanVerticalOffset.dp.toPx()
                            }
                    ) {
                        val isHoleCard = isDealer && index == 1
                        if (isHoleCard) {
                            DealerCard(
                                card = card,
                                isFaceUp = !card.isFaceDown,
                                dealerUpcard = hand.cards.getOrNull(0),
                                dealerScore = hand.score,
                                scale = cardScale,
                            )
                        } else {
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
                                isNearMiss = isNearMiss,
                                isActive = isActive
                            )
                        }
                    }
                }
            }
        }
    ) { measurables, constraints ->
        val childConstraints = constraints.copy(minWidth = 0)
        val placeables = measurables.map { it.measure(childConstraints) }

        val n = placeables.size
        val cardW = placeables.firstOrNull()?.width ?: 0
        val cardH = placeables.maxOfOrNull { it.height } ?: 0

        // Base step size (positive distance from left edge of one card to left edge of next)
        val defaultStepPx = overlapOffset.roundToPx() + cardW

        val maxAvailableW = constraints.maxWidth

        // Determine the horizontal step size dynamically so it fits in the container
        val actualStepPx =
            if (n > 1) {
                val requiredDefaultW = cardW + (n - 1) * defaultStepPx
                if (requiredDefaultW > maxAvailableW) {
                    // Squeeze cards to fit, but reserve at least ~32% of card width for readability
                    val squeezedStep = (maxAvailableW - cardW) / (n - 1)
                    val minStepPx = (cardW * 0.32f).toInt()
                    squeezedStep.coerceAtLeast(minStepPx)
                } else {
                    defaultStepPx
                }
            } else {
                defaultStepPx
            }

        val actualVerticalStepPx = verticalStep.roundToPx()

        // Add padding to account for fanning rotation and translation
        val hPadding = 12.dp.roundToPx()
        val vPadding = 16.dp.roundToPx()

        val totalWidth = if (n == 0) 0 else cardW + (n - 1) * actualStepPx + (hPadding * 2)
        val totalHeight = if (n == 0) 0 else cardH + (n - 1) * actualVerticalStepPx + (vPadding * 2)

        layout(totalWidth.coerceAtLeast(0).coerceAtMost(maxAvailableW), totalHeight.coerceAtLeast(0)) {
            placeables.forEachIndexed { i, placeable ->
                placeable.placeRelative(
                    x = i * actualStepPx + hPadding,
                    y = i * actualVerticalStepPx + vPadding
                )
            }
        }
    }
}
