package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import kotlinx.coroutines.delay

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
    isDealer: Boolean,
    modifier: Modifier = Modifier,
    animationDelay: Int = 0,
    animationDurationMs: Int = 300,
    scale: Float = 1f,
    isNearMiss: Boolean = false,
) {
    val offsetY = remember { Animatable(if (isDealer) -300f else 300f) }
    val nearMissAlpha = remember { Animatable(0f) }

    LaunchedEffect(isNearMiss) {
        if (isNearMiss) {
            nearMissAlpha.animateTo(1f, tween(durationMillis = 300))
            delay(600L)
            nearMissAlpha.animateTo(0f, tween(durationMillis = 600))
        } else {
            nearMissAlpha.snapTo(0f)
        }
    }

    LaunchedEffect(card) {
        delay(animationDelay.toLong())
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = animationDurationMs, easing = LinearOutSlowInEasing),
        )
    }

    val transition = updateTransition(targetState = isFaceUp, label = "cardFlip")
    val rotation by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        },
        label = "rotation",
    ) { faceUp ->
        if (faceUp) 180f else 0f
    }

    val showBack = rotation < 90f

    Box(
        modifier =
            modifier
                .width((120 * scale).dp)
                .aspectRatio(24f / 34f)
                .graphicsLayer {
                    translationY = offsetY.value
                    rotationY = rotation
                    cameraDistance = 12f * density
                },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(
                        width = if (nearMissAlpha.value > 0f) 2.dp else 0.5.dp,
                        color =
                            if (nearMissAlpha.value > 0f) {
                                PrimaryGold.copy(alpha = nearMissAlpha.value)
                            } else {
                                Color.Black.copy(alpha = 0.1f)
                            },
                        shape = RoundedCornerShape(8.dp)
                    ),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            if (!showBack) {
                // Face
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .graphicsLayer { rotationY = 180f },
                ) {
                    // Top Left
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // Center
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color.copy(alpha = 0.08f),
                            fontSize = 64.sp,
                            style = MaterialTheme.typography.displayLarge,
                        )
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 32.sp,
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }

                    // Bottom Right
                    Column(
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .graphicsLayer { rotationZ = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = card.rank.symbol,
                            color = card.suit.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = card.suit.symbol,
                            color = card.suit.color,
                            fontSize = 14.sp,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                // Back
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(FeltDark)
                            .drawWithCache {
                                // Pre-build the checkerboard path to avoid repeated for-loops in onDrawBehind
                                val cellSize = 8.dp.toPx()
                                val checkerPath =
                                    androidx.compose.ui.graphics
                                        .Path()
                                for (x in 0..(size.width / cellSize).toInt()) {
                                    for (y in 0..(size.height / cellSize).toInt()) {
                                        if ((x + y) % 2 == 0) {
                                            checkerPath.addRect(
                                                androidx.compose.ui.geometry.Rect(
                                                    Offset(x * cellSize, y * cellSize),
                                                    androidx.compose.ui.geometry
                                                        .Size(cellSize, cellSize)
                                                )
                                            )
                                        }
                                    }
                                }

                                onDrawBehind {
                                    drawPath(
                                        path = checkerPath,
                                        color = PrimaryGold.copy(alpha = 0.05f)
                                    )

                                    drawRoundRect(
                                        color = Color.White.copy(alpha = 0.2f),
                                        size = size,
                                        cornerRadius = CornerRadius(6.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )
                                }
                            },
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "👑",
                            fontSize = 40.sp,
                        )
                    }
                }
            }
        }
    }
}
