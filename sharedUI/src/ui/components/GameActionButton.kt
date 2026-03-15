package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

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
    val shadowColor = if (enabled) Color.Black.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier =
            modifier
                .alpha(if (enabled) 1f else 0.5f)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.then(
                    if (enabled) {
                        Modifier.shadow(
                            elevation = if (isPressed) 1.dp else 4.dp,
                            shape = CircleShape,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    } else {
                        Modifier
                    }
                ).clip(CircleShape)
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    baseColor,
                                    Color(
                                        (baseColor.red * 0.85f).coerceIn(0f, 1f),
                                        (baseColor.green * 0.85f).coerceIn(0f, 1f),
                                        (baseColor.blue * 0.85f).coerceIn(0f, 1f),
                                        baseColor.alpha
                                    )
                                )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(GlassDark, GlassDark)
                        )
                    }
                ).then(
                    if (enabled) {
                        Modifier.border(
                            width = 1.dp,
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.2f)
                                        )
                                ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color =
                                if (isStrategic) {
                                    PrimaryGold.copy(alpha = 0.2f)
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
                            shape = CircleShape
                        )
                    }
                ).clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ).padding(vertical = 8.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val disabledContentColor =
            if (isStrategic) {
                PrimaryGold.copy(alpha = 0.3f)
            } else {
                resolvedContentColor.copy(alpha = 0.3f)
            }

        val finalColor = if (enabled) resolvedContentColor else disabledContentColor

        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = finalColor,
                modifier = Modifier.size(if (label != null) 24.dp else 32.dp)
            )
            if (label != null) {
                Text(
                    text = label.uppercase(),
                    color = finalColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 8.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
