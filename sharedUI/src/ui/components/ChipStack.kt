package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.random.Random

private val DENOMINATIONS = listOf(500, 100, 50, 25, 10, 5, 1)

@Composable
fun ChipStack(
    amount: Int,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val chips =
        remember(amount) {
            val list = mutableListOf<Int>()
            var remaining = amount
            for (denom in DENOMINATIONS) {
                while (remaining >= denom && list.size < 8) { // Increased to 8 chips in visual stack
                    list.add(denom)
                    remaining -= denom
                }
            }
            list.reversed() // Bottom to top
        }

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
        BetChip(amount = amount, isActive = isActive, modifier = modifier)
    } else {
        Box(modifier = modifier, contentAlignment = Alignment.BottomCenter) {
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

@Preview
@Composable
private fun ChipStackPreview() {
    Box(modifier = Modifier.padding(32.dp)) {
        ChipStack(amount = 86)
    }
}

@Preview
@Composable
private fun ChipStackActivePreview() {
    Box(modifier = Modifier.padding(32.dp)) {
        ChipStack(amount = 250, isActive = true)
    }
}
