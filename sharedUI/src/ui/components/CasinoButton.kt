package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 20.dp),
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "buttonScale"
    )

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else MaterialTheme.colorScheme.secondary
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else MaterialTheme.colorScheme.onSecondary

    // Disabled colors: desaturated and darker
    val baseColor = if (enabled) {
        resolvedContainerColor
    } else {
        // Simple desaturation/darkening for disabled state
        Color(0xFF2A2A2A) 
    }
    
    val shadowColor = if (enabled) Color.Black.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed || !enabled) 1.dp else 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    colors = if (enabled) {
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
                    } else {
                        listOf(baseColor, baseColor)
                    }
                )
            )
            .then(
                if (enabled) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
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
                        color = Color.White.copy(alpha = 0.05f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = if (enabled) resolvedContentColor else resolvedContentColor.copy(alpha = 0.2f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp
        )
    }
}
