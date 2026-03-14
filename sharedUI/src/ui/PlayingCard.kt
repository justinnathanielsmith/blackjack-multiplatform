package io.github.smithjustinn.blackjack.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Rank
import io.github.smithjustinn.blackjack.Suit
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

val Suit.color: Color
    get() =
        when (this) {
            Suit.HEARTS, Suit.DIAMONDS -> PokerRed
            Suit.CLUBS, Suit.SPADES -> PokerBlack
        }

val Suit.symbol: String
    get() =
        when (this) {
            Suit.HEARTS -> "♥"
            Suit.DIAMONDS -> "♦"
            Suit.CLUBS -> "♣"
            Suit.SPADES -> "♠"
        }

val Rank.symbol: String
    get() =
        when (this) {
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
    val appearScale = remember { Animatable(0f) }
    val dealOffset = remember { Animatable(-200f) }

    LaunchedEffect(Unit) {
        appearScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }

    LaunchedEffect(Unit) {
        dealOffset.animateTo(0f, spring(stiffness = Spring.StiffnessLow))
    }

    val transition = updateTransition(targetState = isFaceUp, label = "cardFlip")
    val rotation by transition.animateFloat(
        transitionSpec = {
            spring<Float>(
                stiffness = Spring.StiffnessLow,
                dampingRatio = Spring.DampingRatioLowBouncy
            )
        },
        label = "rotation"
    ) { faceUp ->
        if (faceUp) 0f else 180f
    }

    val flipElevation by transition.animateDp(
        transitionSpec = {
            spring(stiffness = Spring.StiffnessLow)
        },
        label = "flipElevation"
    ) { faceUp ->
        if (transition.isRunning || transition.targetState != transition.currentState) 12.dp else 6.dp
    }

    Box(
        modifier =
            modifier
                .width(100.dp)
                .aspectRatio(2.5f / 3.5f)
                .offset(y = dealOffset.value.dp)
                .graphicsLayer {
                    scaleX = appearScale.value
                    scaleY = appearScale.value
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = flipElevation)
        ) {
            if (rotation <= 90f) {
                // Face
                Box(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    // Top Left
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    // Center
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color.copy(alpha = 0.08f),
                            fontSize = 64.sp,
                            style = MaterialTheme.typography.displayLarge
                        )
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            style = MaterialTheme.typography.headlineLarge
                        )
                    }

                    // Bottom Right
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .graphicsLayer { rotationZ = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                // Back
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f }
                            .background(FeltDark) // Premium Felt Dark back
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                        // Pattern background
                        val cellSize = 8.dp.toPx()
                        for (x in 0..(size.width / cellSize).toInt()) {
                            for (y in 0..(size.height / cellSize).toInt()) {
                                if ((x + y) % 2 == 0) {
                                    drawRect(
                                        color = PrimaryGold.copy(alpha = 0.05f),
                                        topLeft = Offset(x * cellSize, y * cellSize),
                                        size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                                    )
                                }
                            }
                        }

                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.2f),
                            size = size,
                            cornerRadius = CornerRadius(6.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                    // Crown / Logo in center
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "👑",
                            fontSize = 40.sp,
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        )
                    }
                }
            }
        }
    }
}
