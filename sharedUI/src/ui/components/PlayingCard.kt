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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Rank
import io.github.smithjustinn.blackjack.Suit
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
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

data class PipPosition(
    val xFraction: Float,
    val yFraction: Float,
    val rotated: Boolean,
)

private val pipLayouts: Map<Rank, List<PipPosition>> =
    mapOf(
        Rank.TWO to
            listOf(
                PipPosition(0.5f, 0.25f, false),
                PipPosition(0.5f, 0.75f, true),
            ),
        Rank.THREE to
            listOf(
                PipPosition(0.5f, 0.18f, false),
                PipPosition(0.5f, 0.50f, false),
                PipPosition(0.5f, 0.82f, true),
            ),
        Rank.FOUR to
            listOf(
                PipPosition(0.25f, 0.25f, false),
                PipPosition(0.75f, 0.25f, false),
                PipPosition(0.25f, 0.75f, true),
                PipPosition(0.75f, 0.75f, true),
            ),
        Rank.FIVE to
            listOf(
                PipPosition(0.25f, 0.25f, false),
                PipPosition(0.75f, 0.25f, false),
                PipPosition(0.5f, 0.50f, false),
                PipPosition(0.25f, 0.75f, true),
                PipPosition(0.75f, 0.75f, true),
            ),
        Rank.SIX to
            listOf(
                PipPosition(0.25f, 0.20f, false),
                PipPosition(0.75f, 0.20f, false),
                PipPosition(0.25f, 0.50f, false),
                PipPosition(0.75f, 0.50f, false),
                PipPosition(0.25f, 0.80f, true),
                PipPosition(0.75f, 0.80f, true),
            ),
        Rank.SEVEN to
            listOf(
                PipPosition(0.25f, 0.20f, false),
                PipPosition(0.75f, 0.20f, false),
                PipPosition(0.25f, 0.50f, false),
                PipPosition(0.75f, 0.50f, false),
                PipPosition(0.25f, 0.80f, true),
                PipPosition(0.75f, 0.80f, true),
                PipPosition(0.5f, 0.33f, false),
            ),
        Rank.EIGHT to
            listOf(
                PipPosition(0.25f, 0.20f, false),
                PipPosition(0.75f, 0.20f, false),
                PipPosition(0.25f, 0.50f, false),
                PipPosition(0.75f, 0.50f, false),
                PipPosition(0.25f, 0.80f, true),
                PipPosition(0.75f, 0.80f, true),
                PipPosition(0.5f, 0.33f, false),
                PipPosition(0.5f, 0.67f, true),
            ),
        Rank.NINE to
            listOf(
                PipPosition(0.25f, 0.15f, false),
                PipPosition(0.75f, 0.15f, false),
                PipPosition(0.25f, 0.38f, false),
                PipPosition(0.75f, 0.38f, false),
                PipPosition(0.5f, 0.50f, false),
                PipPosition(0.25f, 0.62f, true),
                PipPosition(0.75f, 0.62f, true),
                PipPosition(0.25f, 0.85f, true),
                PipPosition(0.75f, 0.85f, true),
            ),
        Rank.TEN to
            listOf(
                PipPosition(0.25f, 0.12f, false),
                PipPosition(0.75f, 0.12f, false),
                PipPosition(0.5f, 0.26f, false),
                PipPosition(0.25f, 0.38f, false),
                PipPosition(0.75f, 0.38f, false),
                PipPosition(0.25f, 0.62f, true),
                PipPosition(0.75f, 0.62f, true),
                PipPosition(0.5f, 0.74f, true),
                PipPosition(0.25f, 0.88f, true),
                PipPosition(0.75f, 0.88f, true),
            ),
    )

@Composable
fun CardFace(
    rank: Rank,
    suit: Suit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        when {
            rank == Rank.ACE -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = suit.symbol,
                        color = suit.color,
                        fontSize = 36.sp,
                    )
                }
            }
            rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = suit.symbol,
                        color = suit.color,
                        fontSize = 52.sp,
                    )
                }
            }
            else -> PipGrid(rank, suit, maxWidth, maxHeight)
        }
    }
}

@Composable
private fun PipGrid(
    rank: Rank,
    suit: Suit,
    areaWidth: Dp,
    areaHeight: Dp
) {
    val positions = pipLayouts[rank] ?: return
    val pipSize = areaWidth * 0.18f
    val fontSize = maxOf(areaWidth.value * 0.16f, 10f).sp

    Box(modifier = Modifier.fillMaxSize()) {
        for (pip in positions) {
            val xOffset = areaWidth * pip.xFraction - pipSize / 2
            val yOffset = areaHeight * pip.yFraction - pipSize / 2
            Box(
                modifier =
                    Modifier
                        .size(pipSize)
                        .offset(x = xOffset, y = yOffset)
                        .then(if (pip.rotated) Modifier.rotate(180f) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = suit.symbol,
                    color = suit.color,
                    fontSize = fontSize,
                )
            }
        }
    }
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
            tween(durationMillis = AnimationConstants.CardFlipDuration, easing = FastOutSlowInEasing)
        },
        label = "rotation",
    ) { faceUp ->
        if (faceUp) 180f else 0f
    }

    val showBack = rotation < 90f

    Box(
        modifier =
            modifier
                .width(Dimensions.Card.StandardWidth * scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
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
                            .padding(8.dp)
                            .graphicsLayer { rotationY = 180f },
                ) {
                    // Top Left Corner
                    CardCorner(
                        rank = card.rank.symbol,
                        suit = card.suit.symbol,
                        color = card.suit.color,
                        modifier = Modifier.align(Alignment.TopStart)
                    )

                    // Center pip grid / face graphic
                    CardFace(
                        rank = card.rank,
                        suit = card.suit,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 32.dp)
                                .align(Alignment.Center),
                    )

                    // Bottom Right Corner - Inverted and mirrored
                    CardCorner(
                        rank = card.rank.symbol,
                        suit = card.suit.symbol,
                        color = card.suit.color,
                        modifier =
                            Modifier
                                .align(Alignment.BottomEnd)
                                .rotate(180f)
                    )
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
                                    // Outer frame
                                    drawRoundRect(
                                        color = PrimaryGold.copy(alpha = 0.15f),
                                        size = size,
                                        cornerRadius = CornerRadius(6.dp.toPx()),
                                        style = Stroke(width = 2.dp.toPx()),
                                    )

                                    // Inner frame
                                    drawRoundRect(
                                        color = PrimaryGold.copy(alpha = 0.08f),
                                        size = size.copy(width = size.width - 8.dp.toPx(), height = size.height - 8.dp.toPx()),
                                        topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                                        cornerRadius = CornerRadius(4.dp.toPx()),
                                        style = Stroke(width = 1.dp.toPx()),
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

@Composable
private fun CardCorner(
    rank: String,
    suit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement =
            androidx.compose.foundation.layout.Arrangement
                .spacedBy(1.dp)
    ) {
        // Fixed width ensures '10' vs 'J' doesn't cause suit jumping/collisions
        Box(
            modifier = Modifier.width(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank,
                color = color,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
        }
        Text(
            text = suit,
            color = color,
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
        )
    }
}
