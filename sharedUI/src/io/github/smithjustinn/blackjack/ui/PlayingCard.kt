package io.github.smithjustinn.blackjack.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Rank
import io.github.smithjustinn.blackjack.Suit

val Suit.color: Color
    get() = when (this) {
        Suit.HEARTS, Suit.DIAMONDS -> Color(0xFFD32F2F)
        Suit.CLUBS, Suit.SPADES -> Color(0xFF212121)
    }

val Suit.symbol: String
    get() = when (this) {
        Suit.HEARTS -> "♥"
        Suit.DIAMONDS -> "♦"
        Suit.CLUBS -> "♣"
        Suit.SPADES -> "♠"
    }

val Rank.symbol: String
    get() = when (this) {
        Rank.TWO -> "2"
        Rank.THREE -> "3"
        Rank.FOUR -> "4"
        Rank.FIVE -> "5"
        Rank.SIX -> "6"
        Rank.SEVEN -> "7"
        Rank.EIGHT -> "8"
        Rank.NINE -> "9"
        Rank.TEN -> "10"
        Rank.JACK -> "J"
        Rank.QUEEN -> "Q"
        Rank.KING -> "K"
        Rank.ACE -> "A"
    }

@Composable
fun PlayingCard(
    card: Card,
    isFaceUp: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(targetState = isFaceUp, label = "cardFlip")
    val rotation by transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy) },
        label = "rotation"
    ) { faceUp ->
        if (faceUp) 0f else 180f
    }

    Box(
        modifier = modifier
            .width(80.dp)
            .aspectRatio(2.5f / 3.5f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            if (rotation <= 90f) {
                // Face
                Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                    // Top Left
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Center
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color.copy(alpha = 0.1f),
                            style = MaterialTheme.typography.displayMedium
                        )
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }

                    // Bottom Right
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .graphicsLayer { rotationZ = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Back
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .background(Color(0xFF0D47A1)) // Dark Blue back
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.5f),
                            size = size,
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                        drawLine(
                            color = Color.White.copy(alpha = 0.2f),
                            start = Offset(size.width, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                }
            }
        }
    }
}
