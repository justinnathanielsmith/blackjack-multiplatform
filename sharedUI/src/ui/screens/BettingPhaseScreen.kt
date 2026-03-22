package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.BettingSlot
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.effects.FlyingChip
import io.github.smithjustinn.blackjack.ui.effects.FlyingChipAnimation
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.side_bet_perfect_pairs_label
import sharedui.generated.resources.side_bet_twenty_one_plus_three_label

@Composable
fun BettingPhaseScreen(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    selectedAmount: Int,
    modifier: Modifier = Modifier,
) {
    val flyingChips = remember { mutableStateListOf<FlyingChip>() }
    var betDisplayOffset by remember { mutableStateOf(Offset.Zero) }
    val sideBetOffsets = remember { mutableStateMapOf<SideBetType, Offset>() }

    // Remove the dimming felt to bring focus to the betting layer while keeping the table visible
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(safeDrawingInsets())
    ) {
        val placeBetOnArea: (GameAction, Offset, Int) -> Unit =
            remember(audioService, component) {
                { action, offset, amount ->
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(action)
                    if (offset != Offset.Zero) {
                        flyingChips.add(
                            FlyingChip(
                                id = (0..Long.MAX_VALUE).random(),
                                startOffset =
                                    Offset(
                                        x = offset.x + (-10..10).random(),
                                        y = offset.y + (-10..10).random()
                                    ),
                                amount = amount,
                                color = ChipUtils.chipColor(amount),
                                textColor = ChipUtils.chipTextColor(amount),
                            ),
                        )
                    }
                }
            }

        // --- Center: Betting Spots floating directly on the table ---
        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .fillMaxWidth()
                    .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BettingSlot(
                    amount = state.sideBets[SideBetType.PERFECT_PAIRS] ?: 0,
                    label = stringResource(Res.string.side_bet_perfect_pairs_label),
                    isSideBet = true,
                    slotSize = 76.dp,
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, selectedAmount),
                            sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero,
                            selectedAmount
                        )
                    },
                    onPositioned = { sideBetOffsets[SideBetType.PERFECT_PAIRS] = it },
                    onDrop = { amount ->
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, amount),
                            sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero,
                            amount
                        )
                    }
                )

                BettingSlot(
                    amount = state.currentBet,
                    label = "",
                    handCount = state.handCount,
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceBet(selectedAmount),
                            betDisplayOffset,
                            selectedAmount
                        )
                    },
                    onPositioned = { betDisplayOffset = it },
                    onDrop = { amount ->
                        placeBetOnArea(
                            GameAction.PlaceBet(amount),
                            betDisplayOffset,
                            amount
                        )
                    }
                )

                BettingSlot(
                    amount = state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: 0,
                    label = stringResource(Res.string.side_bet_twenty_one_plus_three_label),
                    isSideBet = true,
                    slotSize = 76.dp,
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, selectedAmount),
                            sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero,
                            selectedAmount
                        )
                    },
                    onPositioned = { sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] = it },
                    onDrop = { amount ->
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, amount),
                            sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero,
                            amount
                        )
                    }
                )
            }
        }

        // Flying chip animations layer
        flyingChips.forEach { chip ->
            key(chip.id) {
                FlyingChipAnimation(
                    chip = chip,
                    targetOffset = betDisplayOffset,
                    onAnimationEnd = { flyingChips.remove(chip) },
                )
            }
        }
    }
}
