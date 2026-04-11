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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.components.actions.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.chips.BettingSlot
import io.github.smithjustinn.blackjack.ui.effects.FlyingChipAnimation
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.add_seat_description
import sharedui.generated.resources.minus
import sharedui.generated.resources.plus
import sharedui.generated.resources.remove_seat_description
import sharedui.generated.resources.seat_center
import sharedui.generated.resources.seat_label
import sharedui.generated.resources.seat_left
import sharedui.generated.resources.seat_main
import sharedui.generated.resources.seat_right
import sharedui.generated.resources.seats_count_template
import sharedui.generated.resources.seats_label
import sharedui.generated.resources.side_bet_perfect_pairs_label
import sharedui.generated.resources.side_bet_twenty_one_plus_three_label

/**
 * The primary interaction layer for placing bets and configuring the initial hand count.
 *
 * This screen is active during [io.github.smithjustinn.blackjack.GameStatus.BETTING]. It allows
 * the player to:
 * 1. Select the number of participating hands ([io.github.smithjustinn.blackjack.BlackjackConfig.MIN_INITIAL_HANDS]-[io.github.smithjustinn.blackjack.BlackjackConfig.MAX_INITIAL_HANDS]).
 * 2. Place main bets on one or more seats using a drag-and-drop or tap interaction.
 * 3. Place side bets (Perfect Pairs, 21+3).
 * 4. Experience "juicy" feedback via flying chip animations and audio cues.
 *
 * The layout is responsive, adapting to screen width via [BoxWithConstraints]. It uses a
 * parabolic arc to position player seats, mimicking the curvature of a physical casino table.
 *
 * @param handCount The current number of initial seats enabled for the next round.
 * @param sideBets A map containing currently active side-wagers keyed by [SideBetType].
 * @param playerHands The list of [Hand] objects currently held by the player, used to display
 *        existing bet amounts in each seat.
 * @param component The [BlackjackComponent] used to dispatch betting and configuration actions.
 * @param audioService The [AudioService] used to trigger sound effects for chip interactions.
 * @param selectedAmount The value of the chip currently selected in the UI footer.
 * @param modifier [Modifier] applied to the root container of this screen.
 */
@Composable
fun BettingPhaseScreen(
    handCount: Int,
    sideBets: Map<SideBetType, Int>,
    playerHands: List<Hand>,
    component: BlackjackComponent,
    selectedAmount: Int,
    modifier: Modifier = Modifier,
) {
    // Chip pool, offset maps, and launch logic are managed by a dedicated state holder.
    val animationState = rememberBettingAnimationState()

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .windowInsetsPadding(safeDrawingInsets())
    ) {
        val maxWidth = maxWidth
        val handCount = handCount

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
                    amount = sideBets[SideBetType.PERFECT_PAIRS] ?: 0,
                    label = stringResource(Res.string.side_bet_perfect_pairs_label),
                    isSideBet = true,
                    slotSize = sideBetSize,
                    onClick = {
                        val offset = animationState.sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero
                        component.onPlayClick()
                        component.onAction(GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, selectedAmount))
                        animationState.launchChip(offset, offset, selectedAmount)
                    },
                    onLongClick = {
                        component.onPlayClick()
                        component.onAction(GameAction.ResetSideBet(SideBetType.PERFECT_PAIRS))
                    },
                    onPositioned = { animationState.sideBetOffsets[SideBetType.PERFECT_PAIRS] = it },
                )

                // 21+3 side bet
                BettingSlot(
                    amount = sideBets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: 0,
                    label = stringResource(Res.string.side_bet_twenty_one_plus_three_label),
                    isSideBet = true,
                    slotSize = sideBetSize,
                    onClick = {
                        val offset = animationState.sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero
                        component.onPlayClick()
                        component.onAction(GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, selectedAmount))
                        animationState.launchChip(offset, offset, selectedAmount)
                    },
                    onLongClick = {
                        component.onPlayClick()
                        component.onAction(GameAction.ResetSideBet(SideBetType.TWENTY_ONE_PLUS_THREE))
                    },
                    onPositioned = { animationState.sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] = it },
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

                    val seatLabel = stringResource(getSeatLabelResource(handCount, seatIndex))

                    BettingSlot(
                        amount = playerHands.getOrNull(seatIndex)?.bet ?: 0,
                        label = seatLabel,
                        modifier = Modifier.offset(y = arcYOffset),
                        slotSize = seatSize,
                        onClick = {
                            val offset = animationState.betDisplayOffsets[seatIndex] ?: Offset.Zero
                            component.onPlayClick()
                            component.onAction(GameAction.PlaceBet(selectedAmount, seatIndex))
                            animationState.launchChip(offset, offset, selectedAmount)
                        },
                        onLongClick = {
                            component.onPlayClick()
                            component.onAction(GameAction.ResetSeatBet(seatIndex))
                        },
                        onPositioned = { animationState.betDisplayOffsets[seatIndex] = it },
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
                    enabled = handCount > BlackjackConfig.MIN_INITIAL_HANDS,
                    onClick = {
                        component.onAction(GameAction.SelectHandCount(handCount - 1))
                    },
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                )

                AnimatedContent(
                    targetState = handCount,
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
                                if (count ==
                                    1
                                ) {
                                    stringResource(Res.string.seat_label)
                                } else {
                                    stringResource(Res.string.seats_label)
                                }
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
                    enabled = handCount < BlackjackConfig.MAX_INITIAL_HANDS,
                    onClick = {
                        component.onAction(GameAction.SelectHandCount(handCount + 1))
                    },
                    contentPadding =
                        PaddingValues(
                            horizontal = 16.dp,
                            vertical = 10.dp
                        ),
                )
            }
        }

        // Flying chip animations layer
        for (i in 0 until animationState.flyingChips.size) {
            val chip = animationState.flyingChips[i]
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

private fun getSeatLabelResource(
    handCount: Int,
    seatIndex: Int
): StringResource =
    when {
        handCount == 1 -> Res.string.seat_main
        handCount == 2 -> if (seatIndex == 0) Res.string.seat_left else Res.string.seat_right
        else ->
            when (seatIndex) {
                0 -> Res.string.seat_left
                1 -> Res.string.seat_center
                else -> Res.string.seat_right
            }
    }
