package io.github.smithjustinn.blackjack.ui.components.chips

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.logic.ChipLogic
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

@Composable
fun ChipStack(
    amount: Int,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val pulseScale = remember { Animatable(1f) }

    LaunchedEffect(amount) {
        if (amount > 0) {
            pulseScale.snapTo(1.15f)
            pulseScale.animateTo(
                targetValue = 1f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
            )
        }
    }

    val animatedModifier =
        modifier.graphicsLayer {
            scaleX = pulseScale.value
            scaleY = pulseScale.value
        }
    val chips = remember(amount) { ChipLogic.calculateChipStack(amount) }

    // Keep offsets stable for the same amount
    val stackOffsets =
        remember(chips.size) {
            List(chips.size) { index ->
                val angle = (index * 137.5f) * (PI / 180f).toFloat() // Golden angle jitter
                val jitter = 1.0f + (Random.nextFloat() * 1.5f)
                Offset(
                    x = (cos(angle) * jitter),
                    y = -index * 4.5f // slightly taller stack height per chip
                )
            }
        }

    if (chips.isEmpty() && amount > 0) {
        BetChip(amount = amount, isActive = isActive, modifier = animatedModifier)
    } else {
        Box(modifier = animatedModifier, contentAlignment = Alignment.BottomCenter) {
            chips.forEachIndexed { index, denom ->
                val offset = stackOffsets.getOrElse(index) { Offset(0f, -index * 4.5f) }

                BetChip(
                    amount = if (index == chips.lastIndex) amount else denom,
                    chipColor = ChipUtils.chipColor(denom),
                    textColor = ChipUtils.chipTextColor(denom),
                    isActive = isActive,
                    modifier = Modifier.offset(x = offset.x.dp, y = offset.y.dp)
                )
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ChipStackPreview() {
    Box(modifier = Modifier.padding(32.dp)) {
        ChipStack(amount = 86)
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ChipStackActivePreview() {
    Box(modifier = Modifier.padding(32.dp)) {
        ChipStack(amount = 250, isActive = true)
    }
}
