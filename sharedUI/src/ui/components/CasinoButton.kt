package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun CasinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isStrategic: Boolean = false,
    showShine: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout
            .PaddingValues(horizontal = 24.dp, vertical = 20.dp),
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
        label = "buttonScale",
    )

    val shineTransition = rememberInfiniteTransition(label = "buttonShine")
    val shineProgress by shineTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1800, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
        label = "shineProgress"
    )

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else MaterialTheme.colorScheme.secondary
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else MaterialTheme.colorScheme.onSecondary

    // Disabled colors: desaturated and darker
    val baseColor = resolvedContainerColor

    val shadowColor = if (enabled) Color.Black.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.then(
                    if (enabled) {
                        Modifier.shadow(
                            elevation = if (isPressed) 1.dp else 6.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    } else {
                        Modifier
                    }
                ).clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    baseColor,
                                    // Slightly darker version for 3D effect
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
                            colors =
                                listOf(
                                    io.github.smithjustinn.blackjack.ui.theme.GlassDark,
                                    io.github.smithjustinn.blackjack.ui.theme.GlassDark
                                )
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
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color =
                                if (isStrategic) {
                                    io.github.smithjustinn.blackjack.ui.theme.PrimaryGold.copy(
                                        alpha = 0.2f
                                    )
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                ).drawBehind {
                    if (showShine && enabled) {
                        val shineX = -size.width + shineProgress * 3f * size.width
                        val bandWidth = size.width * 0.4f
                        drawRect(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.3f),
                                            Color.Transparent
                                        ),
                                    start = Offset(shineX, 0f),
                                    end = Offset(shineX + bandWidth, size.height)
                                )
                        )
                    }
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick
                ).padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        val disabledContentColor =
            if (isStrategic) {
                io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
                    .copy(alpha = 0.3f)
            } else {
                resolvedContentColor.copy(alpha = 0.3f)
            }

        Text(
            text = text.uppercase(),
            color = if (enabled) resolvedContentColor else disabledContentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp
        )
    }
}
