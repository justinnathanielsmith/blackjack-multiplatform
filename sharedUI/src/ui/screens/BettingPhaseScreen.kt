package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.*
import io.github.smithjustinn.blackjack.ui.components.effects.FlyingChip
import io.github.smithjustinn.blackjack.ui.components.effects.FlyingChipAnimation
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.*

@Composable
fun BettingPhaseScreen(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    modifier: Modifier = Modifier,
) {
    val flyingChips = remember { mutableStateListOf<FlyingChip>() }
    var betDisplayOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAmount by remember { mutableStateOf(10) }
    val sideBetOffsets = remember { mutableStateMapOf<SideBetType, Offset>() }

    // Dim the felt slightly to bring focus to the betting layer
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
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

        val onResetBet =
            remember(audioService, component) {
                {
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.ResetBet)
                    component.onAction(GameAction.ResetSideBets)
                }
            }

        val onDeal =
            remember(audioService, component) {
                {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.Deal)
                }
            }

        val onChipSelected =
            remember(audioService) {
                { amount: Int ->
                    selectedAmount = amount
                    audioService.playEffect(AudioService.SoundEffect.PLINK)
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
            Text(
                text = stringResource(Res.string.status_betting).uppercase(),
                style = MaterialTheme.typography.titleLarge,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 6.sp,
            )

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
                    onPositioned = { sideBetOffsets[SideBetType.PERFECT_PAIRS] = it }
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
                    onPositioned = { betDisplayOffset = it }
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
                    onPositioned = { sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] = it }
                )
            }
        }

        // --- Bottom: Controls in a sleek glass panel ---
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp) // Offset above the ControlCenter
                    .zIndex(2f)
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassDark)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ChipSelector(
                balance = state.balance,
                selectedAmount = selectedAmount,
                onChipSelected = onChipSelected,
            )

            BettingActions(
                canDeal = state.currentBet > 0,
                onReset = onResetBet,
                onDeal = onDeal,
            )
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
