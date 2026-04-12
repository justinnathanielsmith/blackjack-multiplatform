package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Offset
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.ui.components.chips.ChipUtils
import io.github.smithjustinn.blackjack.ui.effects.FlyingChip

// Separates chip animation state from the BettingPhaseScreen composable — single-responsibility state holder.
private const val FLYING_CHIP_POOL_SIZE = 15
private const val CHIP_START_JITTER = 4

/**
 * Encapsulates all flying-chip animation state for the betting phase.
 *
 * Centralises the chip object pool, position offset maps, and the [launchChip] dispatch function
 * so that [BettingPhaseScreen] is responsible only for layout and composition.
 */
class BettingAnimationState(
    val flyingChips: List<FlyingChip>,
    val betDisplayOffsets: SnapshotStateMap<Int, Offset>,
    val sideBetOffsets: SnapshotStateMap<SideBetType, Offset>,
) {
    fun launchChip(
        startOffset: Offset,
        targetOffset: Offset,
        amount: Int,
    ) {
        if (startOffset == Offset.Zero) return
        val chip = flyingChips.firstOrNull { !it.isActive } ?: return

        chip.startOffset =
            Offset(
                x = startOffset.x + (-CHIP_START_JITTER..CHIP_START_JITTER).random(),
                y = startOffset.y + (-CHIP_START_JITTER..CHIP_START_JITTER).random(),
            )
        chip.targetOffset = targetOffset
        chip.amount = amount
        chip.color = ChipUtils.chipColor(amount)
        chip.textColor = ChipUtils.chipTextColor(amount)
        chip.isActive = true
    }
}

@Composable
fun rememberBettingAnimationState(): BettingAnimationState {
    val flyingChips = remember { List(FLYING_CHIP_POOL_SIZE) { FlyingChip(it.toLong()) } }
    val betDisplayOffsets = remember { mutableStateMapOf<Int, Offset>() }
    val sideBetOffsets = remember { mutableStateMapOf<SideBetType, Offset>() }
    return remember(flyingChips, betDisplayOffsets, sideBetOffsets) {
        BettingAnimationState(flyingChips, betDisplayOffsets, sideBetOffsets)
    }
}
