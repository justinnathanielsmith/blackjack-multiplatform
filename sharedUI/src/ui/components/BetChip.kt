package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.components.drawing.drawChipBody
import io.github.smithjustinn.blackjack.ui.components.drawing.drawChipDepth
import io.github.smithjustinn.blackjack.ui.components.drawing.drawChipHighlight
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bet_chip_amount_k
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

    val chipSize by animateDpAsState(
        targetValue = if (isActive) 56.dp else 48.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "chipSizeAnimation"
    )

    val displayAmount =
        when {
            amount >= 1000 -> stringResource(Res.string.bet_chip_amount_k, amount / 1000)
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
                            .selectable(
                                selected = isActive,
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
                ).drawWithCache {
                    val radius = size.minDimension / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    // Increased depth for 3D clay look
                    val depthOffset = 6.dp.toPx()

                    // Pre-compute gradients and strokes (Bolt Performance Optimization)
                    // Avoids allocating Brush, PathEffect, and FloatArray on every draw frame.
                    val mainBrush =
                        Brush.radialGradient(
                            0.0f to chipColor,
                            0.7f to chipColor,
                            1.0f to chipColor.copy(alpha = 0.85f),
                            center = center,
                            radius = radius
                        )

                    val topGlossBrush =
                        Brush.linearGradient(
                            colors =
                                listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.2f)
                                ),
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height)
                        )

                    val outerRimStroke = Stroke(width = 1.dp.toPx())

                    val dashLength = (radius * 2 * PI / 8).toFloat()
                    val dashedStroke =
                        Stroke(
                            width = 6.dp.toPx(),
                            pathEffect =
                                PathEffect.dashPathEffect(
                                    floatArrayOf(dashLength * 0.15f, dashLength * 0.85f),
                                    0f
                                )
                        )

                    val innerHighlightStroke = Stroke(width = 1.5.dp.toPx())

                    onDrawBehind {
                        drawChipDepth(
                            chipColor = chipColor,
                            radius = radius,
                            center = center,
                            depthOffset = depthOffset,
                        )
                        drawChipBody(
                            mainBrush = mainBrush,
                            outerRimStroke = outerRimStroke,
                            dashedStroke = dashedStroke,
                            innerHighlightStroke = innerHighlightStroke,
                            radius = radius,
                            center = center,
                        )
                        drawChipHighlight(
                            topGlossBrush = topGlossBrush,
                            radius = radius,
                            center = center,
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        val amountText = displayAmount
        val textStyle = MaterialTheme.typography.labelSmall
        val fontSize = if (isActive) 15.sp else 13.sp

        Text(
            text = amountText,
            color = if (enabled) textColor else textColor.copy(alpha = 0.4f),
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            style = textStyle.copy(letterSpacing = 0.sp),
            modifier = Modifier.padding(4.dp)
        )

        // Disable overlay
        if (!enabled) {
            Box(
                modifier =
                    Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun BetChipPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BetChip(5, onClick = {})
            Spacer(Modifier.width(8.dp))
            BetChip(25, onClick = {})
            Spacer(Modifier.width(8.dp))
            BetChip(100, isActive = true, onClick = {})
            Spacer(Modifier.width(8.dp))
            BetChip(500, onClick = {})
            Spacer(Modifier.width(8.dp))
            BetChip(1000, enabled = false)
        }
    }
}
