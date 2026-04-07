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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.BadgeNeutralGrey
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.hit
import sharedui.generated.resources.ic_hit
import sharedui.generated.resources.ic_stand
import sharedui.generated.resources.stand

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
    val isFocused by interactionSource.collectIsFocusedAsState()

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
                animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
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
                    role = Role.Button,
                    onClick = onClick
                ).semantics {
                    contentDescription = label ?: ""
                }.then(
                    if (isFocused) {
                        Modifier.border(2.dp, Color.White, CircleShape)
                    } else {
                        Modifier
                    }
                ).padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val radius = size.minDimension / 2
            val center = Offset(size.width / 2, size.height / 2)
            val depthOffset = 2.dp.toPx()

            if (enabled) {
                // Outer Glow for Strategic Actions
                if (isStrategic) {
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = 0.3f), Color.Transparent),
                            radius = radius * 1.5f
                        )
                    drawCircle(brush = glowBrush, radius = radius * 1.5f)
                }

                // Side depth (3D effect)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.4f),
                    radius = radius,
                    center = center.copy(y = center.y + depthOffset)
                )

                // Main body - Metallic Gradient
                val brush =
                    Brush.verticalGradient(
                        listOf(
                            baseColor.blend(Color.White, 0.4f),
                            baseColor,
                            baseColor.blend(Color.Black, 0.3f)
                        )
                    )

                drawCircle(
                    brush = brush,
                    radius = radius,
                    center = center
                )

                // Premium Rim Highlight (Metallic)
                drawCircle(
                    brush =
                        Brush.sweepGradient(
                            0.0f to Color.White.copy(alpha = 0.6f),
                            0.2f to Color.White.copy(alpha = 0.1f),
                            0.5f to Color.White.copy(alpha = 0.6f),
                            0.8f to Color.White.copy(alpha = 0.1f),
                            1.0f to Color.White.copy(alpha = 0.6f)
                        ),
                    radius = radius * 0.96f,
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            } else {
                // Disabled flat look
                drawCircle(
                    color = BadgeNeutralGrey.copy(alpha = 0.5f),
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

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
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

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun GameActionButtonPreview() {
    BlackjackTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement =
                Arrangement
                    .spacedBy(8.dp)
        ) {
            GameActionButton(
                icon = Res.drawable.ic_hit,
                onClick = {},
                label = stringResource(Res.string.hit),
                isStrategic = true
            )
            GameActionButton(
                icon = Res.drawable.ic_stand,
                onClick = {},
                label = stringResource(Res.string.stand)
            )
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
