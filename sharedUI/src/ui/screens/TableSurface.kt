package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.components.drawing.drawBettingArc
import io.github.smithjustinn.blackjack.ui.components.drawing.drawFeltGradient
import io.github.smithjustinn.blackjack.ui.components.drawing.drawFiberTexture
import io.github.smithjustinn.blackjack.ui.components.drawing.drawRails
import io.github.smithjustinn.blackjack.ui.components.drawing.drawVignette
import io.github.smithjustinn.blackjack.ui.theme.FeltDeepEdge
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.FeltWarmCenter
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun TableSurface(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(FeltDeepEdge) // Fallback deep color
                .drawWithCache {
                    // Static size-dependent brushes — recreated only when size changes
                    val feltBrush =
                        Brush.radialGradient(
                            // Deeper, richer felt colors for premium feel
                            colors =
                                listOf(
                                    FeltWarmCenter,
                                    FeltGreen,
                                    FeltDeepEdge,
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                            center = Offset(size.width / 2, size.height * 0.35f),
                            radius = size.maxDimension * 0.65f
                        )
                    val vignetteBrush =
                        Brush.radialGradient(
                            // Dramatically deeper vignette for high roller mood
                            colors =
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.maxDimension * 0.7f
                        )
                    val arcWidth = size.width * 1.5f
                    val arcHeight = size.height * 0.6f
                    val arcLeft = (size.width - arcWidth) / 2
                    val arcTop = size.height * 0.35f
                    val arcSize =
                        Size(arcWidth, arcHeight)
                    val arcStroke =
                        Stroke(width = 3.dp.toPx())
                    val insuranceStroke =
                        Stroke(width = 1.5.dp.toPx())
                    val insuranceOffset = 40.dp.toPx()

                    onDrawBehind {
                        // 1. Base felt gradient
                        drawFeltGradient(feltBrush)
                        // 1b. Fiber texture (subtle noise / linen)
                        drawFiberTexture()
                        // 2 & 3. Betting arc + insurance arc (embossed)
                        drawBettingArc(
                            arcLeft = arcLeft,
                            arcTop = arcTop,
                            arcSize = arcSize,
                            arcStroke = arcStroke,
                            insuranceStroke = insuranceStroke,
                            insuranceOffset = insuranceOffset,
                            primaryGold = PrimaryGold,
                        )
                        // 4. Heavy vignette (dark leather rail mood)
                        drawVignette(vignetteBrush)
                        // 4b. Physical wood table rails
                        drawRails(railHeight = 20.dp.toPx())
                    }
                }
    ) {
        // Table printing added to the felt
        Column(
            modifier = Modifier.fillMaxSize().padding(top = 180.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "BLACKJACK PAYS 3 TO 2",
                color = PrimaryGold.copy(alpha = 0.12f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
            Text(
                text = "Dealer must draw to 16, and stand on all 17s",
                color = Color.White.copy(alpha = 0.08f),
                style = MaterialTheme.typography.bodyMedium,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "INSURANCE PAYS 2 TO 1",
                color = PrimaryGold.copy(alpha = 0.08f),
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
