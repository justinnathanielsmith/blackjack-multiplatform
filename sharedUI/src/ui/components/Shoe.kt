package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun Shoe(
    state: GameState,
    modifier: Modifier = Modifier
) {
    val deckCount = state.rules.deckCount
    val totalCards = deckCount * 52
    val remainingCards = state.deck.size

    val thicknessRatio =
        if (totalCards > 0) {
            remainingCards.toFloat() / totalCards.coerceAtLeast(1)
        } else {
            0f
        }

    val maxLayers = 12
    val visibleLayers = (thicknessRatio * maxLayers).toInt().coerceAtLeast(if (remainingCards > 0) 1 else 0)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // The Shoe Box + Cards
        Box(contentAlignment = Alignment.Center) {
            // Shoe Holder (Physical Box)
            Box(
                modifier =
                    Modifier
                        .size(width = 80.dp, height = 110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            brush =
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF1A1A1A), Color.Black)
                                )
                        ).border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            )

            // Cards within the shoe
            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                val shoeScale = 0.45f
                repeat(visibleLayers) { index ->
                    CardBack(
                        modifier =
                            Modifier
                                .scale(shoeScale)
                                .offset(
                                    x = (index * 1.2).dp,
                                    y = (index * 1.2).dp
                                )
                    )
                }
            }
        }

        // Card Count Label
        ShoeLabel(remaining = remainingCards, total = totalCards)
    }
}

@Composable
private fun ShoeLabel(
    remaining: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val decksLeft = (remaining / 52.0)
    val formattedDecks = if (decksLeft < 1.0) "0.5" else decksLeft.toInt().toString()

    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$remaining / $total CARDS",
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryGold.copy(alpha = 0.9f),
            fontWeight = FontWeight.Black,
            fontSize = 8.sp,
            letterSpacing = 0.5.sp
        )
    }
}
