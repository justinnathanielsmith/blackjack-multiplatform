package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameState

@Composable
fun Shoe(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val deckCount = state.rules.deckCount
    val totalCards = deckCount * 52
    val remainingCards = state.deck.size

    // Calculate how "thick" the shoe should look
    // 0.0 to 1.0 (1.0 being a full 6-deck shoe)
    val thicknessRatio =
        if (totalCards > 0) {
            remainingCards.toFloat() / totalCards.coerceAtLeast(1)
        } else {
            0f
        }

    // Maximum number of visible layers to simulate depth
    // 8 layers for a full shoe feels premium without over-rendering
    val maxLayers = 8
    val visibleLayers = (thicknessRatio * maxLayers).toInt().coerceAtLeast(if (remainingCards > 0) 1 else 0)

    Box(modifier = modifier) {
        // Draw layers from bottom to top, slightly offset to create a 3D effect
        // We scale it down slightly so it's not a full-sized card on the UI
        val shoeScale = 0.5f

        repeat(visibleLayers) { index ->
            CardBack(
                modifier =
                    Modifier
                        .scale(shoeScale)
                        .offset(
                            x = (index * 1.5).dp,
                            y = (index * 1.5).dp
                        )
            )
        }
    }
}
