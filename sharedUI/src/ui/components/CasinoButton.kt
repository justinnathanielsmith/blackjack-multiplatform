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

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else MaterialTheme.colorScheme.secondary
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else MaterialTheme.colorScheme.onSecondary

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }.clip(RoundedCornerShape(16.dp))
                .background(resolvedContainerColor)
                .shadow(
                    elevation = if (isPressed) 2.dp else 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black
                ).border(
                    width = 1.dp,
                    brush =
                        Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ).clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ).padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            color = resolvedContentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp
        )
    }
}
