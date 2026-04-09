package io.github.smithjustinn.blackjack.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

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
