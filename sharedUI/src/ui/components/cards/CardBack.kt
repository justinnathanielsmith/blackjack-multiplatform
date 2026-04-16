package io.github.smithjustinn.blackjack.ui.components.cards

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.DeepWine
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.suit_spades

@Composable
fun CardBack(modifier: Modifier = Modifier) {
    // Bolt Performance Optimization: Pre-allocate the medallion brush once for the composable
    // lifetime. The Canvas draw scope has no cache mechanism, so this brush would otherwise be
    // re-allocated 60× per second per face-down card (dealer hole card + any undealt cards).
    // This brush has no dependency on size or state — it is safe and correct to remember it here.
    val medallionBrush =
        remember {
            Brush.linearGradient(
                colors = listOf(ModernGoldLight, ModernGoldDark)
            )
        }
    Box(
        modifier =
            modifier
                .requiredWidth(Dimensions.Card.StandardWidth)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .background(Color.White)
                .padding(4.dp)
                // 2. Rich premium casino core
                .background(
                    Brush.linearGradient(
                        colors =
                            listOf(
                                DeepWine,
                                Color(0xFF2C0A0A) // Deeper wine for gradient
                            )
                    ),
                    RoundedCornerShape(4.dp)
                ).clip(RoundedCornerShape(4.dp))
                .drawWithCache {
                    val spacing = 8.dp.toPx()
                    val strokeWidth = 1.dp.toPx()
                    val patternColor = ModernGoldDark.copy(alpha = 0.3f)

                    // Bolt Performance Optimization: Hoist the inner-frame gradient brush into
                    // the drawWithCache scope. It was previously inside onDrawBehind, allocating a
                    // new Brush.linearGradient on every frame for every face-down card on the table
                    // (dealer hole card during dealing / playing phases). The cache scope re-runs
                    // only when `size` changes, reducing allocations from O(Frames) to O(SizeChanges).
                    val innerFrameBrush = Brush.linearGradient(colors = listOf(ModernGoldLight, ModernGoldDark))
                    val innerFrameStroke = Stroke(width = 1.5.dp.toPx())
                    val innerFrameCornerRadius = CornerRadius(4.dp.toPx())
                    val innerFrameMargin = 6.dp.toPx()

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

                        // 4. Inner gold foil frame — brush and stroke pre-computed in cache scope above
                        drawRoundRect(
                            brush = innerFrameBrush,
                            size =
                                size.copy(
                                    width = size.width - innerFrameMargin * 2,
                                    height = size.height - innerFrameMargin * 2,
                                ),
                            topLeft = Offset(innerFrameMargin, innerFrameMargin),
                            cornerRadius = innerFrameCornerRadius,
                            style = innerFrameStroke,
                        )
                    }
                },
    ) {
        // 5. Center Casino Medallion
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Bolt Performance Optimization: Use pre-allocated `medallionBrush` (remembered
            // at composable scope above). Canvas.drawScope has no cache mechanism, so without
            // this, a new Brush.linearGradient is created on every frame per face-down card.
            Canvas(modifier = Modifier.size(36.dp)) {
                val centerOffset = Offset(size.width / 2, size.height / 2)

                // Outer gold ring — uses `medallionBrush` pre-allocated in composable scope
                drawCircle(
                    brush = medallionBrush,
                    radius = size.minDimension / 2,
                    center = centerOffset
                )
                // Inner dark core
                drawCircle(
                    color = DeepWine,
                    radius = size.minDimension / 2 - 2.dp.toPx(),
                    center = centerOffset
                )
                // Delicate inner gold detail
                drawCircle(
                    color = ModernGoldDark.copy(alpha = 0.8f),
                    radius = size.minDimension / 2 - 5.dp.toPx(),
                    center = centerOffset,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            // Center icon (Spade)
            Text(
                text = stringResource(Res.string.suit_spades),
                fontSize = 18.sp,
                color = ModernGoldLight
            )
        }
    }
}
