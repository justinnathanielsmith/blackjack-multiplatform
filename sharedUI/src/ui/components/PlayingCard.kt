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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
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

@Composable
fun CardFace(
    rank: Rank,
    suit: Suit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Slot machine style: single large bold rank centered
        // '10' is two characters, so scale it down slightly to avoid corner overlap
        val isTen = rank == Rank.TEN
        val scaleFactor = if (isTen) 0.55f else 0.7f
        val minSize = if (isTen) 30f else 40f

        val fontSizeRank = maxOf(maxWidth.value * scaleFactor, minSize).sp
        Text(
            text = rank.symbol,
            color = suit.color,
            fontWeight = FontWeight.Black,
            fontSize = fontSizeRank,
            style = androidx.compose.ui.text.TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.15f),
                    offset = Offset(2f, 2f),
                    blurRadius = 4f
                )
            )
        )
    }
}

@Composable
fun PlayingCard(
    card: io.github.smithjustinn.blackjack.Card,
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
                .requiredWidth(Dimensions.Card.StandardWidth * scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(8.dp), clip = false)
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
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                ) {
                    val cardWidth = maxWidth
                    val isSmall = cardWidth < 65.dp

                    val cornerPadding = if (isSmall) 4.dp else 6.dp

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(cornerPadding)
                    ) {
                        // Faint watermark suit symbol in the center
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.suit.symbol,
                                color = card.suit.color.copy(alpha = if (isSmall) 0.15f else 0.08f),
                                fontSize = (cardWidth.value * if (isSmall) 1.2f else 1.0f).sp,
                            )
                        }

                        if (isSmall) {
                            // Compact: Only Top-Left Jumbo Index for maximum clarity
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = true,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        } else {
                            // Normal: Top Left Corner
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = false,
                                modifier = Modifier.align(Alignment.TopStart)
                            )

                            // Center slot-machine graphic
                            CardFace(
                                rank = card.rank,
                                suit = card.suit,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Bottom Right Corner - Inverted and mirrored
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = false,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .rotate(180f)
                            )
                        }
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
                                        size =
                                            size.copy(
                                                width = size.width - 8.dp.toPx(),
                                                height =
                                                    size.height - 8.dp.toPx()
                                            ),
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
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy((-4).dp)
    ) {
        // Fixed width ensures '10' vs 'J' doesn't cause suit jumping/collisions
        Box(
            modifier = Modifier.width(if (isSmall) 26.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank,
                color = color,
                fontWeight = FontWeight.Black,
                fontSize = if (isSmall) 22.sp else 24.sp,
                maxLines = 1,
            )
        }
        Text(
            text = suit,
            color = color,
            fontSize = if (isSmall) 18.sp else 20.sp,
            maxLines = 1,
        )
    }
}
