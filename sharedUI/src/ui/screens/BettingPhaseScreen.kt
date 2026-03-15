package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import io.github.smithjustinn.blackjack.ui.components.ChipUtils
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bet_multiplier
import sharedui.generated.resources.bet_total_label
import sharedui.generated.resources.deal
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.status_betting

private fun formatCurrency(amount: Int): String = "$$amount"

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

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
                .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        val placeBetOnArea: (GameAction, Offset, Int) -> Unit = { action, offset, amount ->
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
                        // Slight jitter for multiple chips
                        amount = amount,
                        color = ChipUtils.chipColor(amount),
                        textColor = ChipUtils.chipTextColor(amount),
                    ),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .zIndex(1f)
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(GlassDark)
                    .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                    .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(Res.string.status_betting).uppercase(),
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
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
                    modifier =
                        Modifier.onGloballyPositioned {
                            sideBetOffsets[SideBetType.PERFECT_PAIRS] =
                                it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        }
                )

                BetDisplayCard(
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
                    modifier = Modifier.weight(1f)
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
                    modifier =
                        Modifier.onGloballyPositioned {
                            sideBetOffsets[SideBetType.TWENTY_ONE_PLUS_THREE] =
                                it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        }
                )
            }

            ChipSelector(
                balance = state.balance,
                selectedAmount = selectedAmount,
                onChipSelected = { selectedAmount = it },
            )

            BettingActions(
                canDeal = state.currentBet > 0,
                onReset = {
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.ResetBet)
                    component.onAction(GameAction.ResetSideBets)
                },
                onDeal = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.Deal)
                },
            )
        }

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
private fun BetDisplayCard(
    currentBet: Int,
    handCount: Int,
    onPositioned: (Offset) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalBet = currentBet * handCount
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp),
                ).clickable { onClick() }
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .onGloballyPositioned {
                    val center = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                    onPositioned(center)
                },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.bet_total_label).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGold.copy(alpha = 0.8f),
                letterSpacing = 2.sp,
            )
            if (handCount > 1) {
                Text(
                    text = stringResource(Res.string.bet_multiplier, handCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold,
                    modifier =
                        Modifier
                            .background(PrimaryGold.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
        AnimatedContent(
            targetState = totalBet,
            transitionSpec = {
                fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "betAmount",
        ) { bet ->
            val scale by animateFloatAsState(
                targetValue = if (bet > 0) 1.05f else 1f,
                animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f),
                label = "betScale"
            )
            Text(
                text = formatCurrency(bet),
                style = MaterialTheme.typography.displayMedium,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                modifier =
                    Modifier.graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
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
    val label =
        when (type) {
            SideBetType.PERFECT_PAIRS -> "PP"
            SideBetType.TWENTY_ONE_PLUS_THREE -> "21+3"
        }

    val infiniteTransition = rememberInfiniteTransition(label = "sideBetGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (amount > 0) 0.8f else 0.3f,
        animationSpec =
            infiniteRepeatable(
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
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = if (amount > 0) 2.dp else 1.dp,
                        color = PrimaryGold.copy(alpha = if (amount > 0) glowAlpha else 0.3f),
                        shape = CircleShape
                    ).shadow(
                        elevation = if (amount > 0) (8 * glowAlpha).dp else 0.dp,
                        shape = CircleShape,
                        spotColor = PrimaryGold.copy(alpha = if (amount > 0) glowAlpha else 0f)
                    ).clickable { onClick() },
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
                    text = label,
                    color = PrimaryGold.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
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
        animationSpec =
            infiniteRepeatable(
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
            containerColor = Color.Transparent,
            contentColor = Color.White.copy(alpha = 0.7f),
        )
        CasinoButton(
            text = stringResource(Res.string.deal),
            onClick = onDeal,
            modifier =
                Modifier.weight(1f).graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            enabled = canDeal,
            isStrategic = true,
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
        // Phase 1: glide to just above the target (180ms)
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

        // Phase 2: fall onto the stack (220ms, gravity easing)
        animY.animateTo(
            targetValue = targetOffset.y,
            animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing),
        )

        // Phase 3: squash on landing
        launch {
            scaleX.animateTo(1.2f, animationSpec = tween(50))
            scaleX.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }
        launch {
            scaleY.animateTo(0.8f, animationSpec = tween(50))
            scaleY.animateTo(1f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f))
        }

        // Phase 4: fade out after brief pause
        delay(120)
        alpha.animateTo(0f, animationSpec = tween(180))
        onAnimationEnd()
    }

    Box(
        modifier =
            Modifier
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
