package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlin.math.PI

@Composable
fun BetChip(
    amount: Int,
    modifier: Modifier = Modifier,
    chipColor: Color = PrimaryGold,
    textColor: Color = FeltDark,
    isActive: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec =
            if (isPressed) {
                tween(durationMillis = 80)
            } else {
                spring(dampingRatio = 0.4f, stiffness = 400f)
            },
        label = "chipScale",
    )

    val chipSize = if (isActive) 56.dp else 48.dp

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.size(chipSize)
                .shadow(
                    elevation = if (isActive) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = if (isActive) chipColor.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f),
                ).then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            enabled = enabled,
                            onClick = onClick,
                        )
                    } else {
                        Modifier
                    }
                ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Side depth offset (3D effect)
            val depthOffset = 3.dp.toPx()

            // Draw the "side" of the chip for 3D depth
            drawCircle(
                color = chipColor.copy(alpha = 0.7f),
                radius = radius,
                center = center.copy(y = center.y + depthOffset)
            )

            // Main top surface
            drawCircle(
                color = chipColor,
                radius = radius,
                center = center
            )

            // Outer rim highlights
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Decorative blocks on the rim (standard casino chip look)
            val dashLength = (radius * 2 * PI / 12).toFloat()
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = radius * 0.92f,
                center = center,
                style =
                    Stroke(
                        width = 4.dp.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(dashLength / 2, dashLength / 2),
                                0f
                            )
                    )
            )

            // Inner circle highlight
            drawCircle(
                color = Color.Black.copy(alpha = 0.1f),
                radius = radius * 0.75f,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Center inlay
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius * 0.65f,
                center = center
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            val displayAmount =
                when {
                    amount >= 1000 -> "${amount / 1000}K"
                    else -> amount.toString()
                }

            Text(
                text = displayAmount,
                color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
                fontSize = if (isActive) 15.sp else 13.sp,
                fontWeight = FontWeight.Black,
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.sp
                    )
            )
        }

        // Disable overlay
        if (!enabled) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }
}
