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
import io.github.smithjustinn.blackjack.ui.theme.ModernGold

@Composable
fun CasinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isStrategic: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "buttonScale"
    )

    val alpha = if (enabled) 1f else 0.5f

    // Strategic buttons have a metallic gold fill/glow per DESIGN.md
    val resolvedContainerColor = containerColor ?: if (isStrategic) ModernGold else MaterialTheme.colorScheme.secondary
    val resolvedContentColor = contentColor ?: if (isStrategic) Color.Black else MaterialTheme.colorScheme.onSecondary

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }.shadow(
                    elevation = if (isPressed) 2.dp else 8.dp,
                    shape = RoundedCornerShape(12.dp),
                    ambientColor = if (isStrategic) ModernGold.copy(alpha = 0.5f) else Color.Black,
                    spotColor = if (isStrategic) ModernGold else Color.Black
                ).clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            resolvedContainerColor.copy(alpha = 1f),
                            resolvedContainerColor.copy(alpha = 0.85f),
                            resolvedContainerColor.copy(alpha = 0.7f)
                        )
                    )
                ).border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = if (isStrategic) {
                            listOf(Color.White.copy(alpha = 0.3f), Color.Transparent)
                        } else {
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent)
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ).clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ).padding(horizontal = 24.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = resolvedContentColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 0.5.sp
        )
    }
}
