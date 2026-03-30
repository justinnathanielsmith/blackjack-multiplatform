package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.BettingSlot
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.effects.FlyingChip
import io.github.smithjustinn.blackjack.ui.effects.FlyingChipAnimation
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.add_seat_description
import sharedui.generated.resources.minus
import sharedui.generated.resources.plus
import sharedui.generated.resources.remove_seat_description
import sharedui.generated.resources.seat_center
import sharedui.generated.resources.seat_left
import sharedui.generated.resources.seat_main
import sharedui.generated.resources.seat_right
import sharedui.generated.resources.seats_count_template
import sharedui.generated.resources.seats_label
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
    val flyingChips = remember { List(15) { FlyingChip(it.toLong()) } }
    val betDisplayOffsets = remember { mutableStateMapOf<Int, Offset>() }
    val sideBetOffsets = remember { mutableStateMapOf<SideBetType, Offset>() }

    fun launchChip(
        startOffset: Offset,
        targetOffset: Offset,
        amount: Int
    ) {
        if (startOffset == Offset.Zero) return
        val chip = flyingChips.firstOrNull { !it.isActive } ?: return

        chip.startOffset =
            Offset(
                x = startOffset.x + (-10..10).random(),
                y = startOffset.y + (-10..10).random(),
            )
        chip.targetOffset = targetOffset
        chip.amount = amount
        chip.color = ChipUtils.chipColor(amount)
        chip.textColor = ChipUtils.chipTextColor(amount)
        chip.isActive = true
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(safeDrawingInsets())
    ) {
        val maxWidth = maxWidth
        val handCount = state.handCount

        // Calculate responsive sizes
        val isNarrow = maxWidth < 500.dp
        val isTiny = maxWidth < 380.dp

        val seatSize =
            when {
                isTiny && handCount == 3 -> 84.dp
                isNarrow && handCount == 3 -> 96.dp
                isNarrow && handCount == 2 -> 110.dp
                else -> 124.dp
            }

        val sideBetSize =
            when {
                isTiny && handCount == 3 -> 56.dp
                isNarrow && handCount == 3 -> 64.dp
                isNarrow -> 68.dp
                else -> 76.dp
            }

        val outerSpacing =
            when {
                isTiny && handCount == 3 -> 8.dp
                isNarrow && handCount == 3 -> 12.dp
                isNarrow -> 16.dp
                else -> 24.dp
            }

        val innerSpacing =
            when {
                isTiny && handCount == 3 -> 4.dp
                isNarrow && handCount == 3 -> 8.dp
                isNarrow -> 12.dp
                else -> 16.dp
            }

        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .fillMaxWidth()
                    .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // ── Side bets + seat arc ──────────────────────────────────────────
            // ── Side bets ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Perfect Pairs side bet
                BettingSlot(
                    amount = state.sideBets[SideBetType.PERFECT_PAIRS] ?: 0,
                    label = stringResource(Res.string.side_bet_perfect_pairs_label),
                    isSideBet = true,
                    slotSize = sideBetSize,
                    onClick = {
                        val offset = sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, selectedAmount))
                        launchChip(offset, offset, selectedAmount)
                    },
                    onPositioned = { sideBetOffsets[SideBetType.PERFECT_PAIRS] = it },
                    onDrop = { amount ->
                        val offset = sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, amount))
                        launchChip(offset, offset, amount)
                    },
                    onHoverChange = { isHovered ->
                        if (isHovered) audioService.playEffect(AudioService.SoundEffect.PLINK)
                    }
                )

                // 21+3 side bet
                BettingSlot(
                    amount = state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: 0,
                    label = stringResource(Res.string.side_bet_twenty_one_plus_three_label),
                    isSideBet = true,
                    slotSize = sideBetSize,
                    onClick = {
                        val offset = sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, selectedAmount))
                        launchChip(offset, offset, selectedAmount)
                    },
                    onPositioned = { sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] = it },
                    onDrop = { amount ->
                        val offset = sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, amount))
                        launchChip(offset, offset, amount)
                    },
                    onHoverChange = { isHovered ->
                        if (isHovered) audioService.playEffect(AudioService.SoundEffect.PLINK)
                    }
                )
            }

            // ── Seat betting circles (arc layout) ────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(innerSpacing, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for (seatIndex in 0 until handCount) {
                    // Parabolic arc: outer seats dip lower, echoing the card table arc
                    val relPos =
                        if (handCount > 1) {
                            (seatIndex / (handCount - 1f)) * 2f - 1f
                        } else {
                            0f
                        }
                    val arcYOffset = (relPos * relPos * (if (isNarrow) 10 else 14)).dp

                    val seatLabel =
                        when {
                            handCount == 1 -> stringResource(Res.string.seat_main)
                            handCount == 2 ->
                                if (seatIndex ==
                                    0
                                ) {
                                    stringResource(Res.string.seat_left)
                                } else {
                                    stringResource(Res.string.seat_right)
                                }
                            else ->
                                when (seatIndex) {
                                    0 -> stringResource(Res.string.seat_left)
                                    1 -> stringResource(Res.string.seat_center)
                                    else -> stringResource(Res.string.seat_right)
                                }
                        }

                    BettingSlot(
                        amount = state.playerHands.getOrNull(seatIndex)?.bet ?: 0,
                        label = seatLabel,
                        modifier = Modifier.offset(y = arcYOffset),
                        slotSize = seatSize,
                        onClick = {
                            val offset = betDisplayOffsets[seatIndex] ?: Offset.Zero
                            audioService.playEffect(AudioService.SoundEffect.CLICK)
                            component.onAction(GameAction.PlaceBet(selectedAmount, seatIndex))
                            launchChip(offset, offset, selectedAmount)
                        },
                        onPositioned = { betDisplayOffsets[seatIndex] = it },
                        onDrop = { amount ->
                            val offset = betDisplayOffsets[seatIndex] ?: Offset.Zero
                            audioService.playEffect(AudioService.SoundEffect.CLICK)
                            component.onAction(GameAction.PlaceBet(amount, seatIndex))
                            launchChip(offset, offset, amount)
                        },
                        onHoverChange = { isHovered ->
                            if (isHovered) audioService.playEffect(AudioService.SoundEffect.PLINK)
                        }
                    )
                }
            }

            // ── Seat count selector ───────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CasinoButton(
                    text = stringResource(Res.string.minus),
                    contentDescription = stringResource(Res.string.remove_seat_description),
                    enabled = state.handCount > 1,
                    onClick = {
                        component.onAction(GameAction.SelectHandCount(state.handCount - 1))
                    },
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                )

                AnimatedContent(
                    targetState = state.handCount,
                    transitionSpec = {
                        if (targetState > initialState) {
                            // Sliding up when count increases
                            (slideInVertically { height -> height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            // Sliding down when count decreases
                            (slideInVertically { height -> -height } + fadeIn())
                                .togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }.using(
                            sizeTransform = null // Don't animate size changes of the container
                        )
                    },
                    label = "SeatCountAnimation"
                ) { count ->
                    Text(
                        text =
                            stringResource(
                                Res.string.seats_count_template,
                                count,
                                stringResource(Res.string.seats_label)
                            ),
                        style = MaterialTheme.typography.labelLarge,
                        color = PrimaryGold,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }

                CasinoButton(
                    text = stringResource(Res.string.plus),
                    contentDescription = stringResource(Res.string.add_seat_description),
                    enabled = state.handCount < 3,
                    onClick = {
                        component.onAction(GameAction.SelectHandCount(state.handCount + 1))
                    },
                    contentPadding =
                        androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                )
            }
        }

        // Flying chip animations layer
        for (i in 0 until flyingChips.size) {
            val chip = flyingChips[i]
            if (chip.isActive) {
                key(chip.id) {
                    FlyingChipAnimation(
                        chip = chip,
                        targetOffset = chip.targetOffset,
                        onAnimationEnd = { chip.isActive = false },
                    )
                }
            }
        }
    }
}
