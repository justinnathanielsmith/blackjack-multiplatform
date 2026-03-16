package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.PI

@Composable
fun GameActionButton(
    icon: DrawableResource,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isStrategic: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    label: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val baseScale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec =
            if (isPressed) {
                tween(durationMillis = 80)
            } else {
                spring(dampingRatio = 0.4f, stiffness = 400f)
            },
        label = "actionButtonScale",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isStrategic && enabled && !isPressed) 1.05f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathScale",
    )

    val scale = baseScale * if (isStrategic && enabled) breathingScale else 1.0f

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else GlassDark
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else Color.White

    val baseColor = resolvedContainerColor

    Box(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.5f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)
            val depthOffset = 2.dp.toPx()

            if (enabled) {
                // Side depth
                drawCircle(
                    color = baseColor.copy(alpha = 0.5f),
                    radius = radius,
                    center = center.copy(y = center.y + depthOffset)
                )

                // Main chip body with subtle vertical gradient for 3D feel
                drawCircle(
                    brush =
                        Brush.verticalGradient(
                            listOf(
                                baseColor.copy(alpha = 1f).blend(Color.White, 0.1f),
                                baseColor,
                                baseColor.copy(alpha = 1f).blend(Color.Black, 0.1f)
                            )
                        ),
                    radius = radius,
                    center = center
                )

                // Dashed rim
                val dashLength = (radius * 2 * Math.PI / 16).toFloat()
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = radius * 0.92f,
                    center = center,
                    style =
                        Stroke(
                            width = 3.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(dashLength / 2, dashLength / 2),
                                    0f
                                )
                        )
                )

                // Inner embossed ring
                drawCircle(
                    color = Color.Black.copy(alpha = 0.15f),
                    radius = radius * 0.78f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Top highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius * 0.76f,
                    center = center,
                    style = Stroke(width = 0.5.dp.toPx())
                )
            } else {
                // Disabled flat look
                drawCircle(
                    color = GlassDark.copy(alpha = 0.5f),
                    radius = radius,
                    center = center
                )
            }
        }

        val disabledContentColor =
            if (isStrategic) {
                PrimaryGold.copy(alpha = 0.3f)
            } else {
                resolvedContentColor.copy(alpha = 0.3f)
            }

        val finalColor = if (enabled) resolvedContentColor else disabledContentColor

        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            modifier = Modifier.padding(4.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = finalColor,
                modifier = Modifier.size(if (label != null) 20.dp else 28.dp)
            )
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    color = finalColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

private fun Color.blend(
    other: Color,
    amount: Float
): Color {
    return Color(
        red = (red + (other.red - red) * amount).coerceIn(0f, 1f),
        green = (green + (other.green - green) * amount).coerceIn(0f, 1f),
        blue = (blue + (other.blue - blue) * amount).coerceIn(0f, 1f),
        alpha = alpha + (other.alpha - alpha) * amount
    )
}
