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

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.size(if (isActive) 56.dp else 48.dp) // Smaller for hand display, larger for active
                .shadow(
                    elevation = if (isActive) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = Color.Black,
                    spotColor = if (isActive) PrimaryGold else Color.Black,
                ).clip(CircleShape)
                .then(
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

            // Main body
            drawCircle(
                color = if (isActive) chipColor else chipColor.copy(alpha = 0.8f),
                radius = radius,
                center = center
            )

            // Inner ring
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = radius * 0.82f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Outer decorative ring (dashed)
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = radius * 0.9f,
                center = center,
                style =
                    Stroke(
                        width = 3.dp.toPx(),
                        pathEffect =
                            PathEffect.dashPathEffect(
                                floatArrayOf(10f, 10f),
                                0f
                            )
                    )
            )

            // Center circle for value
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                radius = radius * 0.6f,
                center = center
            )
        }

        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = amount.toString(),
                color = if (enabled) textColor else textColor.copy(alpha = 0.5f),
                fontSize = if (isActive) 14.sp else 12.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Disable overlay
        if (!enabled) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
            )
        }
    }
}
