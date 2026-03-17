package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.BetChip
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.ChipSelector
import io.github.smithjustinn.blackjack.ui.components.ChipStack
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bet_multiplier
import sharedui.generated.resources.bet_spot_tap_to_bet
import sharedui.generated.resources.deal
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.side_bet_perfect_pairs_label
import sharedui.generated.resources.side_bet_twenty_one_plus_three_label
import sharedui.generated.resources.status_betting

data class FlyingChip(
    val id: Long,
    val startOffset: Offset,
    val amount: Int,
    val color: Color,
    val textColor: Color
)

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
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .windowInsetsPadding(safeDrawingInsets())
    ) {
        val placeBetOnArea: (GameAction, Offset, Int) -> Unit = remember(audioService, component) {
            { action, offset, amount ->
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(action)
                if (offset != Offset.Zero) {
                    flyingChips.add(
                        FlyingChip(
                            id = (0..Long.MAX_VALUE).random(),
                            startOffset = Offset(
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

        val onResetBet = remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.ResetBet)
                component.onAction(GameAction.ResetSideBets)
            }
        }

        val onDeal = remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.FLIP)
                component.onAction(GameAction.Deal)
            }
        }

        val onChipSelected = remember(audioService) {
            { amount: Int ->
                selectedAmount = amount
                audioService.playEffect(AudioService.SoundEffect.PLINK)
            }
        }

        // --- Center: Betting Spots floating directly on the table ---
        Column(
            modifier = Modifier
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
                SideBetSlot(
                    type = SideBetType.PERFECT_PAIRS,
                    amount = state.sideBets[SideBetType.PERFECT_PAIRS] ?: 0,
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.PERFECT_PAIRS, selectedAmount),
                            sideBetOffsets[SideBetType.PERFECT_PAIRS] ?: Offset.Zero,
                            selectedAmount
                        )
                    },
                    modifier = Modifier.onGloballyPositioned {
                        sideBetOffsets[SideBetType.PERFECT_PAIRS] =
                            it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                    }
                )

                BetSpot(
                    currentBet = state.currentBet,
                    handCount = state.handCount,
                    onPositioned = { betDisplayOffset = it },
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceBet(selectedAmount),
                            betDisplayOffset,
                            selectedAmount
                        )
                    },
                )

                SideBetSlot(
                    type = SideBetType.TWENTY_ONE_PLUS_THREE,
                    amount = state.sideBets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: 0,
                    onClick = {
                        placeBetOnArea(
                            GameAction.PlaceSideBet(SideBetType.TWENTY_ONE_PLUS_THREE, selectedAmount),
                            sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] ?: Offset.Zero,
                            selectedAmount
                        )
                    },
                    modifier = Modifier.onGloballyPositioned {
                        sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] =
                            it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                    }
                )
            }
        }

        // --- Bottom: Controls in a sleek glass panel ---
        Column(
            modifier = Modifier
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

@Composable
private fun BetSpot(
    currentBet: Int,
    handCount: Int,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "betSpotGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (currentBet > 0) 1.0f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "betSpotGlowAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .drawBehind {
                    val strokeWidth = 2.dp.toPx()
                    val outerDash = PathEffect.dashPathEffect(floatArrayOf(24f, 16f), 0f)
                    
                    // Outer Dashed Circle
                    drawCircle(
                        color = PrimaryGold.copy(alpha = if (currentBet > 0) glowAlpha else 0.4f),
                        style = Stroke(width = strokeWidth, pathEffect = outerDash),
                        radius = size.minDimension / 2f
                    )
                    // Inner Thin Circle
                    drawCircle(
                        color = PrimaryGold.copy(alpha = 0.15f),
                        style = Stroke(width = 1.dp.toPx()),
                        radius = size.minDimension / 2.3f
                    )
                }
                .clip(CircleShape)
                .clickable(role = Role.Button) { onClick() }
                .semantics {
                    contentDescription = if (currentBet > 0) "Bet spot with $currentBet" else "Tap to place bet"
                }
                .onGloballyPositioned {
                    val center = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                    onPositioned(center)
                },
            contentAlignment = Alignment.Center
        ) {
            if (currentBet > 0) {
                ChipStack(amount = currentBet, isActive = true)
            } else {
                Text(
                    text = stringResource(Res.string.bet_spot_tap_to_bet).replace(" ", "\n"),
                    color = PrimaryGold.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (handCount > 1) {
            Text(
                text = stringResource(Res.string.bet_multiplier, handCount),
                style = MaterialTheme.typography.labelSmall,
                color = BackgroundDark,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .background(PrimaryGold, RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SideBetSlot(
    type: SideBetType,
    amount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = when (type) {
        SideBetType.PERFECT_PAIRS -> stringResource(Res.string.side_bet_perfect_pairs_label)
        SideBetType.TWENTY_ONE_PLUS_THREE -> stringResource(Res.string.side_bet_twenty_one_plus_three_label)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "sideBetGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (amount > 0) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = if (amount > 0) glowAlpha else 0.25f),
                        style = Stroke(
                            width = if (amount > 0) 2.dp.toPx() else 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
                        ),
                        radius = size.minDimension / 2f
                    )
                }
                .clip(CircleShape)
                .clickable(role = Role.Button) { onClick() }
                .semantics {
                    contentDescription = if (amount > 0) "Side bet $label with $amount" else "Tap to place $label side bet"
                },
            contentAlignment = Alignment.Center
        ) {
            if (amount > 0) {
                BetChip(
                    amount = amount,
                    chipColor = ChipUtils.chipColor(amount),
                    textColor = ChipUtils.chipTextColor(amount),
                )
            } else {
                Text(
                    text = label.replace(" ", "\n"),
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun BettingActions(
    canDeal: Boolean,
    onReset: () -> Unit,
    onDeal: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (canDeal) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dealPulse"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
    ) {
        CasinoButton(
            text = stringResource(Res.string.reset_bet),
            onClick = onReset,
            modifier = Modifier.weight(1f),
            containerColor = GlassDark,
            contentColor = Color.White,
        )
        CasinoButton(
            text = stringResource(Res.string.deal),
            onClick = onDeal,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            enabled = canDeal,
            isStrategic = true,
            showShine = canDeal,
            containerColor = PrimaryGold,
            contentColor = BackgroundDark,
        )
    }
}

@Composable
private fun FlyingChipAnimation(
    chip: FlyingChip,
    targetOffset: Offset,
    onAnimationEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val dropHeight = with(density) { 64.dp.toPx() }

    val animX = remember { Animatable(chip.startOffset.x) }
    val animY = remember { Animatable(chip.startOffset.y) }
    val scaleX = remember { Animatable(1f) }
    val scaleY = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        val aboveY = targetOffset.y - dropHeight
        launch {
            animX.animateTo(
                targetValue = targetOffset.x,
                animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            )
        }
        animY.animateTo(
            targetValue = aboveY,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        )

        animY.animateTo(
            targetValue = targetOffset.y,
            animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
        )

        launch {
            scaleX.animateTo(1.2f, animationSpec = tween(50))
            scaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }
        launch {
            scaleY.animateTo(0.8f, animationSpec = tween(50))
            scaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }

        delay(120)
        alpha.animateTo(0f, animationSpec = tween(180))
        onAnimationEnd()
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(animX.value.toInt(), animY.value.toInt()) }
            .graphicsLayer {
                this.alpha = alpha.value
                this.scaleX = scaleX.value
                this.scaleY = scaleY.value
            },
    ) {
        BetChip(
            amount = chip.amount,
            chipColor = chip.color,
            textColor = chip.textColor,
        )
    }
}
