package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Rank
import io.github.smithjustinn.blackjack.Suit
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed

internal val CardShape = RoundedCornerShape(8.dp)

internal fun shadowStyle(
    color: Color,
    offset: Offset,
    blur: Float
) = TextStyle(shadow = Shadow(color = color, offset = offset, blurRadius = blur))

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
    val isCourt = rank == Rank.JACK || rank == Rank.QUEEN || rank == Rank.KING
    val isAce = rank == Rank.ACE
    val isTen = rank == Rank.TEN

    BoxWithConstraints(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val cardWidth = maxWidth

        // 1. Linen Texture Overlay (Refined Crosshatch)
        Box(
            modifier =
                Modifier.fillMaxSize().drawWithCache {
                    val spacing = 1.5.dp.toPx()
                    val strokeWidth = 0.5.dp.toPx()
                    val darkTexture = Color.Black.copy(alpha = 0.04f)
                    val lightTexture = Color.White.copy(alpha = 0.02f)
                    onDrawBehind {
                        // Horizontal threads
                        for (y in 0..(size.height / spacing).toInt()) {
                            val color = if (y % 2 == 0) darkTexture else lightTexture
                            drawLine(
                                color = color,
                                start = Offset(0f, y * spacing),
                                end = Offset(size.width, y * spacing),
                                strokeWidth = strokeWidth
                            )
                        }
                        // Vertical threads
                        for (x in 0..(size.width / spacing).toInt()) {
                            val color = if (x % 2 == 0) darkTexture else lightTexture
                            drawLine(
                                color = color,
                                start = Offset(x * spacing, 0f),
                                end = Offset(x * spacing, size.height),
                                strokeWidth = strokeWidth
                            )
                        }
                    }
                }
        )

        // 2. Elegant Inner Frame
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 1.dp.toPx()

            // Outer fine line
            drawRoundRect(
                color = if (isCourt) PrimaryGold.copy(alpha = 0.6f) else suit.color.copy(alpha = 0.2f),
                size = size,
                cornerRadius = CornerRadius(4.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        }

        // 3. Centerpiece Design
        if (isCourt) {
            // Court Cards: Gold Foil Medallion
            Box(
                modifier =
                    Modifier
                        .size((cardWidth.value * 0.7f).dp)
                        .drawWithCache {
                            val brushCenter = Offset(size.width / 2, size.height / 2)
                            val ringBrush =
                                Brush.radialGradient(
                                    colors = listOf(PrimaryGold, PrimaryGold.copy(alpha = 0.7f)),
                                    center = brushCenter,
                                    radius = size.minDimension / 2
                                )
                            val shineBrush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                    start = Offset.Zero,
                                    end = Offset(size.width, size.height)
                                )
                            val strokeWidth = 1.5.dp.toPx()
                            onDrawBehind {
                                drawCircle(brush = ringBrush, center = brushCenter, style = Stroke(width = strokeWidth))
                                drawCircle(brush = shineBrush, center = brushCenter)
                            }
                        },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.symbol,
                    color = PrimaryGold,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = (cardWidth.value * Dimensions.Card.CourtRankScale).sp,
                    style = shadowStyle(Color.Black.copy(alpha = 0.4f), Offset(2f, 2f), 6f)
                )
            }
        } else if (isAce) {
            // Aces: Oversized Power Pip
            Text(
                text = suit.symbol,
                color = suit.color,
                fontSize = (cardWidth.value * Dimensions.Card.AcePipScale).sp,
                style = shadowStyle(suit.color.copy(alpha = 0.3f), Offset(0f, 6f), 12f)
            )
        } else {
            // Number Cards: Clean Rank + Pip Stack
            val scaleFactor = if (isTen) Dimensions.Card.NumberRankScaleTen else Dimensions.Card.NumberRankScaleNormal
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = rank.symbol,
                    color = suit.color,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = (cardWidth.value * scaleFactor).sp,
                    style = shadowStyle(Color.Black.copy(alpha = 0.15f), Offset(2f, 2f), 4f),
                    letterSpacing = if (isTen) (-2).sp else 0.sp,
                    softWrap = false,
                    modifier =
                        Modifier.graphicsLayer {
                            if (isTen) scaleX = 0.85f
                        }
                )
                Text(
                    text = suit.symbol,
                    color = suit.color,
                    fontSize = (cardWidth.value * Dimensions.Card.NumberPipScale).sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Box(
        modifier =
            modifier
                .requiredWidth(Dimensions.Card.StandardWidth)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .background(Color.White)
                .padding(4.dp)
                // 2. Rich casino red core
                .background(TacticalRed, RoundedCornerShape(4.dp))
                .clip(RoundedCornerShape(4.dp))
                .drawWithCache {
                    val spacing = 6.dp.toPx()
                    val strokeWidth = 1.dp.toPx()
                    val patternColor = Color.White.copy(alpha = 0.15f)

                    onDrawBehind {
                        // 3. Elegant diamond lattice pattern
                        // Optimized: Only draw lines that actually cross the card area
                        val width = size.width
                        val height = size.height

                        var i = -height
                        while (i < width) {
                            // Diagonal lines top-left to bottom-right ( \ )
                            drawLine(
                                color = patternColor,
                                start = Offset(i, 0f),
                                end = Offset(i + height, height),
                                strokeWidth = strokeWidth
                            )
                            // Diagonal lines bottom-left to top-right ( / )
                            drawLine(
                                color = patternColor,
                                start = Offset(i, height),
                                end = Offset(i + height, 0f),
                                strokeWidth = strokeWidth
                            )
                            i += spacing
                        }

                        // 4. Inner gold foil frame
                        drawRoundRect(
                            color = PrimaryGold.copy(alpha = 0.8f),
                            size =
                                size.copy(
                                    width = size.width - 12.dp.toPx(),
                                    height =
                                        size.height - 12.dp.toPx()
                                ),
                            topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                },
    ) {
        // 5. Center Casino Medallion
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(36.dp)) {
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Outer gold ring
                drawCircle(
                    color = PrimaryGold,
                    radius = size.minDimension / 2,
                    center = centerOffset
                )
                // Inner red core
                drawCircle(
                    color = TacticalRed,
                    radius = size.minDimension / 2 - 2.dp.toPx(),
                    center = centerOffset
                )
                // Delicate inner gold detail
                drawCircle(
                    color = PrimaryGold.copy(alpha = 0.5f),
                    radius = size.minDimension / 2 - 4.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            // Center icon (Spade)
            Text(
                text = "♠",
                fontSize = 18.sp,
                color = PrimaryGold
            )
        }
    }
}

@Composable
fun PlayingCard(
    card: io.github.smithjustinn.blackjack.Card,
    isFaceUp: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    isNearMiss: Boolean = false,
    shadowElevation: androidx.compose.ui.unit.Dp = 6.dp,
    spotColor: Color = Color.Black,
) {
    val baseRotation =
        remember(card) {
            val hash = card.hashCode()
            ((hash % 100) / 100f) * 4f - 2f
        }

    val nearMissAlpha = remember { Animatable(0f) }

    LaunchedEffect(isNearMiss) {
        if (isNearMiss) {
            nearMissAlpha.animateTo(1f, tween(durationMillis = AnimationConstants.NearMissInDuration))
            kotlinx.coroutines.delay(AnimationConstants.NearMissHoldDuration)
            nearMissAlpha.animateTo(0f, tween(durationMillis = AnimationConstants.NearMissOutDuration))
        } else {
            nearMissAlpha.snapTo(0f)
        }
    }

    val transition = updateTransition(targetState = isFaceUp, label = "cardFlip")
    val rotation by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = AnimationConstants.CardFlipDuration, easing = FastOutSlowInEasing)
        },
        label = "rotation",
    ) { faceUp -> if (faceUp) 180f else 0f }

    // Subtle lift effect as the card is flipped
    val liftScale by transition.animateFloat(
        transitionSpec = {
            keyframes {
                durationMillis = AnimationConstants.CardFlipDuration
                1.0f at 0
                1.08f at AnimationConstants.CardFlipDuration / 2
                1.0f at AnimationConstants.CardFlipDuration
            }
        },
        label = "liftScale"
    ) { 1.0f }

    val showBack = rotation < 90f

    Box(
        modifier =
            modifier
                .requiredWidth(Dimensions.Card.StandardWidth * scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    rotationZ = baseRotation
                    rotationY = rotation
                    scaleX = liftScale
                    scaleY = liftScale
                    cameraDistance = 12f * density
                },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxSize()
                    .shadow(elevation = shadowElevation, shape = CardShape, spotColor = spotColor)
                    .drawWithContent {
                        drawContent()
                        val alpha = nearMissAlpha.value
                        val borderWidth = if (alpha > 0f) 2.dp.toPx() else 0.5.dp.toPx()
                        val borderColor =
                            if (alpha > 0f) PrimaryGold.copy(alpha = alpha) else Color.Black.copy(alpha = 0.1f)
                        drawRoundRect(
                            color = borderColor,
                            size = size,
                            cornerRadius = CornerRadius(8.dp.toPx()),
                            style = Stroke(width = borderWidth)
                        )
                    },
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            if (!showBack) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                ) {
                    val cardWidth = maxWidth
                    val isSmall = cardWidth < Dimensions.Card.SmallCardThreshold
                    val cornerPadding = if (isSmall) 4.dp else 6.dp

                    Box(modifier = Modifier.fillMaxSize().padding(cornerPadding)) {
                        if (isSmall) {
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = true,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        } else {
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = false,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            CardFace(
                                rank = card.rank,
                                suit = card.suit,
                                modifier = Modifier.fillMaxSize()
                            )
                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = false,
                                modifier = Modifier.align(Alignment.BottomEnd).rotate(180f)
                            )
                        }
                    }
                }
            } else {
                CardBack(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
internal fun CardCorner(
    rank: String,
    suit: String,
    color: Color,
    isSmall: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy((-4).dp)
    ) {
        // Art Deco condensed effect for '10'
        Box(
            modifier = Modifier.width(if (isSmall) 26.dp else 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank,
                color = color,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = if (isSmall) 22.sp else 24.sp,
                maxLines = 1,
                letterSpacing = if (rank == "10") (-1.5).sp else 0.sp,
                softWrap = false,
                modifier =
                    Modifier.graphicsLayer {
                        if (rank == "10") scaleX = 0.8f
                    }
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
