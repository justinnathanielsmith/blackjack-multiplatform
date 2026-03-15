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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun GameActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isStrategic: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
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
        label = "actionButtonScale",
    )

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else GlassDark
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else Color.White

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
                            elevation = if (isPressed) 1.dp else 4.dp,
                            shape = RoundedCornerShape(12.dp),
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    } else {
                        Modifier
                    }
                ).clip(RoundedCornerShape(12.dp))
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
                            shape = RoundedCornerShape(12.dp)
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
                            shape = RoundedCornerShape(12.dp)
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

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = icon,
                color = finalColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                softWrap = false,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
            )
            Text(
                text = label.uppercase(),
                color = finalColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
