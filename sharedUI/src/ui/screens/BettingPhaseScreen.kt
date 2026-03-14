package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.BetChip
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.components.ChipSelector
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.WhiteSoft
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.bet
import sharedui.generated.resources.deal
import sharedui.generated.resources.hand_count_label
import sharedui.generated.resources.reset_bet
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.total_bet

private fun formatCurrency(amount: Int): String = "$$amount"

data class FlyingChip(
    val id: Long,
    val startOffset: Offset,
    val amount: Int,
    val color: Color,
    val textColor: Color
)

private fun chipColor(value: Int) =
    when (value) {
        10 -> PokerRed
        50 -> PrimaryGold
        100 -> PokerBlack
        else -> PokerBlack
    }

private fun chipTextColor(value: Int) =
    when (value) {
        50 -> FeltDark
        else -> WhiteSoft
    }

@Suppress("LongMethod")
@Composable
fun BettingPhaseScreen(
    state: GameState,
    component: BlackjackComponent,
    audioService: AudioService,
    isCompact: Boolean,
    modifier: Modifier = Modifier,
) {
    require(state.status == GameStatus.BETTING)
    val flyingChips = remember { mutableStateListOf<FlyingChip>() }
    var betDisplayOffset by remember { mutableStateOf(Offset.Zero) }
    var previousBalance by remember { mutableStateOf(state.balance) }

    val animatedBalance by androidx.compose.animation.core.animateIntAsState(
        targetValue = state.balance,
        animationSpec =
            androidx.compose.animation.core.tween(
                durationMillis = if (kotlin.math.abs(state.balance - previousBalance) > 200) 600 else 300,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            ),
        label = "balanceRoll",
        finishedListener = { previousBalance = it },
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.status_betting).uppercase(),
                style =
                    if (isCompact) {
                        MaterialTheme.typography.headlineSmall
                    } else {
                        MaterialTheme.typography.headlineMedium
                    },
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Glassmorphic Info Card
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(if (isCompact) 1f else 0.8f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(GlassDark)
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(24.dp),
                        ).padding(24.dp)
                        .onGloballyPositioned {
                            betDisplayOffset =
                                it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                        },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(Res.string.balance).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 2.sp,
                )
                Text(
                    text = formatCurrency(animatedBalance),
                    style =
                        if (isCompact) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(Res.string.bet).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryGold.copy(alpha = 0.8f),
                    letterSpacing = 2.sp,
                )
                AnimatedContent(
                    targetState = state.currentBet,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "betAmount",
                ) { bet ->
                    Text(
                        text = formatCurrency(bet),
                        style =
                            if (isCompact) {
                                MaterialTheme.typography.displaySmall
                            } else {
                                MaterialTheme.typography.displayMedium
                            },
                        color = PrimaryGold,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            ChipSelector(
                balance = state.balance,
                onBetClick = { amount, offset ->
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.PlaceBet(amount))
                    if (offset != Offset.Zero) {
                        flyingChips.add(
                            FlyingChip(
                                id = (0..Long.MAX_VALUE).random(),
                                startOffset = offset,
                                amount = amount,
                                color = chipColor(amount),
                                textColor = chipTextColor(amount),
                            ),
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            HandCountSelector(
                handCount = state.handCount,
                currentBet = state.currentBet,
                onSelectCount = { count ->
                    audioService.playEffect(AudioService.SoundEffect.CLICK)
                    component.onAction(GameAction.SelectHandCount(count))
                },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
            ) {
                CasinoButton(
                    text = stringResource(Res.string.reset_bet),
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.ResetBet)
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentColor = Color.White.copy(alpha = 0.7f),
                )
                CasinoButton(
                    text = stringResource(Res.string.deal),
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.FLIP)
                        component.onAction(GameAction.Deal)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = state.currentBet > 0,
                    isStrategic = true,
                )
            }
        }

        // Animated Chips Overlay
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
private fun HandCountSelector(
    handCount: Int,
    currentBet: Int,
    onSelectCount: (Int) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(Res.string.hand_count_label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f),
            letterSpacing = 2.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(1, 2, 3).forEach { count ->
                CasinoButton(
                    text = "$count",
                    onClick = { onSelectCount(count) },
                    isStrategic = handCount == count,
                    containerColor = Color.Transparent,
                    contentColor = if (handCount == count) PrimaryGold else Color.White.copy(alpha = 0.5f),
                )
            }
        }
        if (handCount > 1 && currentBet > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    stringResource(
                        Res.string.total_bet,
                        formatCurrency(currentBet * handCount),
                    ),
                style = MaterialTheme.typography.labelMedium,
                color = PrimaryGold.copy(alpha = 0.8f),
                letterSpacing = 1.sp,
            )
        }
    }
}

@Composable
private fun FlyingChipAnimation(
    chip: FlyingChip,
    targetOffset: Offset,
    onAnimationEnd: () -> Unit,
) {
    val density = LocalDensity.current
    val animOffset = remember { Animatable(chip.startOffset, Offset.VectorConverter) }
    val alpha = remember { Animatable(1f) }
    val scale = remember { Animatable(1f) }
    val arcY = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            animOffset.animateTo(
                targetValue = targetOffset,
                animationSpec = tween(durationMillis = 350, easing = FastOutLinearInEasing),
            )
        }
        launch {
            arcY.animateTo(
                targetValue = -100f,
                animationSpec =
                    keyframes {
                        durationMillis = 350
                        -100f at 175
                        0f at 350
                    },
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = 350
                        1f at 250
                        0f at 350
                    },
            )
        }
        launch {
            scale.animateTo(
                targetValue = 0.6f,
                animationSpec = tween(durationMillis = 350),
            )
        }
        delay(350)
        onAnimationEnd()
    }

    val currentOffset = animOffset.value + Offset(0f, arcY.value * density.density)

    Box(
        modifier =
            Modifier
                .offset { currentOffset.round() }
                .graphicsLayer {
                    this.alpha = alpha.value
                    this.scaleX = scale.value
                    this.scaleY = scale.value
                },
    ) {
        BetChip(
            amount = chip.amount,
            chipColor = chip.color,
            textColor = chip.textColor,
        )
    }
}
