package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.DropTarget
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bet_multiplier
import sharedui.generated.resources.bet_spot_description
import sharedui.generated.resources.bet_spot_tap_to_bet
import sharedui.generated.resources.side_bet_spot_description
import sharedui.generated.resources.tap_to_place_bet
import sharedui.generated.resources.tap_to_place_side_bet

@Composable
fun BettingSlot(
    amount: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    slotSize: Dp = 124.dp,
    isSideBet: Boolean = false,
    handCount: Int = 1,
    onPositioned: (Offset) -> Unit = {},
    onDrop: (Int) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bettingSlotGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = if (isSideBet) 0.15f else 0.15f,
        targetValue = if (amount > 0) (if (isSideBet) 0.5f else 0.7f) else (if (isSideBet) 0.15f else 0.15f),
        animationSpec =
            infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "glowAlpha"
    )

    val primaryColor = if (isSideBet) Color.White else PrimaryGold
    val dashEffect =
        if (isSideBet) {
            PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f)
        } else {
            null // Solid painted line for the main bet
        }

    val currentDescription =
        if (amount > 0) {
            if (isSideBet) {
                stringResource(Res.string.side_bet_spot_description, label, amount)
            } else {
                stringResource(Res.string.bet_spot_description, amount)
            }
        } else {
            if (isSideBet) {
                stringResource(Res.string.tap_to_place_side_bet, label)
            } else {
                stringResource(Res.string.tap_to_place_bet)
            }
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(if (isSideBet) 4.dp else 10.dp)
    ) {
        DropTarget(
            onDrop = { item -> if (item is Int) onDrop(item) }
        ) { isHovered ->
            Box(
                modifier =
                    Modifier
                        .size(slotSize)
                        .drawBehind {
                            val strokeWidth =
                                if (isSideBet &&
                                    amount > 0
                                ) {
                                    2.dp.toPx()
                                } else {
                                    (if (isSideBet) 1.dp.toPx() else 2.dp.toPx())
                                }

                            val activeColor = if (isHovered) Color.White else primaryColor

                            // Outer Dashed Circle
                            drawCircle(
                                color = activeColor.copy(alpha = if (isHovered) 0.8f else glowAlpha),
                                style = Stroke(width = strokeWidth + (if (isHovered) 2.dp.toPx() else 0f), pathEffect = dashEffect),
                                radius = size.minDimension / 2f
                            )

                            if (!isSideBet) {
                                // Inner Thin Circle for main bet
                                drawCircle(
                                    color = activeColor.copy(alpha = if (isHovered) 0.3f else 0.15f),
                                    style = Stroke(width = 1.dp.toPx()),
                                    radius = size.minDimension / 2.3f
                                )
                            }
                        }.clip(CircleShape)
                        .clickable(role = Role.Button) { onClick() }
                        .semantics {
                            contentDescription = currentDescription
                        }.onGloballyPositioned {
                            val center = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                            onPositioned(center)
                        },
                contentAlignment = Alignment.Center
            ) {
                if (amount > 0) {
                    if (isSideBet) {
                        BetChip(
                            amount = amount,
                            chipColor = ChipUtils.chipColor(amount),
                            textColor = ChipUtils.chipTextColor(amount),
                        )
                    } else {
                        ChipStack(amount = amount, isActive = true)
                    }
                } else {
                    Text(
                        text =
                            if (isSideBet) {
                                label.replace(
                                    " ",
                                    "\n"
                                )
                            } else {
                                stringResource(Res.string.bet_spot_tap_to_bet).replace(" ", "\n")
                            },
                        color = primaryColor.copy(alpha = if (isSideBet) 0.5f else 0.7f),
                        style =
                            if (isSideBet) {
                                MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp)
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = if (isSideBet) 0.sp else 2.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (!isSideBet && handCount > 1) {
            Text(
                text = stringResource(Res.string.bet_multiplier, handCount),
                style = MaterialTheme.typography.labelSmall,
                color = BackgroundDark,
                fontWeight = FontWeight.Black,
                modifier =
                    Modifier
                        .background(PrimaryGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Preview
@Composable
private fun BettingSlotMainBetPreview() {
    BettingSlot(
        amount = 100,
        label = "",
        onClick = {},
        isSideBet = false,
        handCount = 1
    )
}

@Preview
@Composable
private fun BettingSlotSideBetPreview() {
    BettingSlot(
        amount = 50,
        label = "Perfect Pairs",
        onClick = {},
        isSideBet = true,
        handCount = 1
    )
}

@Preview
@Composable
private fun BettingSlotEmptyMainBetPreview() {
    BettingSlot(
        amount = 0,
        label = "",
        onClick = {},
        isSideBet = false,
        handCount = 1
    )
}

@Preview
@Composable
private fun BettingSlotEmptySideBetPreview() {
    BettingSlot(
        amount = 0,
        label = "Perfect Pairs",
        onClick = {},
        isSideBet = true,
        handCount = 1
    )
}
