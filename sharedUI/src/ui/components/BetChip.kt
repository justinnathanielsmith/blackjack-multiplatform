package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bet_chip_description
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
    val isFocused by interactionSource.collectIsFocusedAsState()

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

    val displayAmount =
        when {
            amount >= 1000 -> "${amount / 1000}K"
            else -> amount.toString()
        }

    val chipDescription = stringResource(Res.string.bet_chip_description, displayAmount)

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
                    if (isFocused) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                ).then(
                    if (onClick != null) {
                        Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                enabled = enabled,
                                role = Role.Button,
                                onClick = onClick,
                            ).semantics {
                                contentDescription = chipDescription
                            }
                    } else {
                        Modifier
                    }
                ).then(
                    if (isFocused) {
                        Modifier.border(2.dp, Color.White, CircleShape)
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
                color = chipColor.copy(alpha = 0.8f),
                radius = radius,
                center = center.copy(y = center.y + depthOffset)
            )

            // Main top surface with a subtle gradient for gloss
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0.0f to chipColor.copy(alpha = 1f),
                        0.7f to chipColor.copy(alpha = 0.95f),
                        1.0f to chipColor.copy(alpha = 0.9f),
                        center = center,
                        radius = radius
                    ),
                radius = radius,
                center = center
            )

            // Gloss highlight at the top
            drawCircle(
                brush =
                    Brush.radialGradient(
                        0.0f to Color.White.copy(alpha = 0.25f),
                        1.0f to Color.Transparent,
                        center = center.copy(y = center.y - radius * 0.4f),
                        radius = radius * 0.6f
                    ),
                radius = radius * 0.6f,
                center = center.copy(y = center.y - radius * 0.4f)
            )

            // Outer rim highlights
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = radius - 0.5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // Decorative blocks on the rim (standard casino chip look)
            val dashLength = (radius * 2 * PI / 12).toFloat()
            if (dashLength > 0f) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = radius * 0.94f,
                    center = center,
                    style =
                        Stroke(
                            width = 3.5.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(dashLength / 2, dashLength / 2),
                                    0f
                                )
                        )
                )
            }

            // Inner circle highlight (recessed look)
            drawCircle(
                color = Color.Black.copy(alpha = 0.15f),
                radius = radius * 0.75f,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )

            // Center inlay
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
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
