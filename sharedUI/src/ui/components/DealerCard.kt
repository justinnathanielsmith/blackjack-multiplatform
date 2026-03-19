package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Rank
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import kotlinx.coroutines.delay

enum class RevealState {
    Hidden,
    Peeking,
    Flipping,
    Revealed
}

@Composable
fun DealerCard(
    card: Card,
    isFaceUp: Boolean,
    dealerUpcard: Card?,
    dealerScore: Int,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    shadowElevation: androidx.compose.ui.unit.Dp = 6.dp,
) {
    val rotationY = remember { Animatable(0f) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Tension Logic: Slow Roll if upcard is Ace/10 and hole card completes Blackjack
    val isBlackjack = dealerScore == 21 && card.rank.value + (dealerUpcard?.rank?.value ?: 0) == 21
    val isTensionVisible = dealerUpcard?.rank == Rank.ACE || dealerUpcard?.rank?.value == 10
    val isSlowRoll = isBlackjack && isTensionVisible

    LaunchedEffect(isFaceUp) {
        if (isFaceUp) {
            // 1. Peeking Phase
            rotationY.animateTo(
                targetValue = 20f,
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
            )

            // 2. Pause + Heartbeat Haptics
            val pauseDuration = if (isSlowRoll) 1200L else 500L
            val heartbeatCount = if (isSlowRoll) 3 else 1

            repeat(heartbeatCount) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(200)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(pauseDuration / heartbeatCount)
            }

            // 3. The Slam
            rotationY.animateTo(
                targetValue = 180f,
                animationSpec =
                    tween(
                        durationMillis = 400,
                        // Use a manual overshoot logic or just a snappy tween
                        // High-velocity tween with overshoot bounce
                    )
            )
            // Final Slam Haptic
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            rotationY.snapTo(0f)
        }
    }

    val showBack = rotationY.value < 90f

    Box(
        modifier =
            modifier
                .requiredWidth(Dimensions.Card.StandardWidth * scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    cameraDistance = 15f * density.density
                },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 0.5.dp,
                        color = Color.Black.copy(alpha = 0.1f),
                        shape = CardShape
                    ),
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = shadowElevation),
        ) {
            if (!showBack) {
                // Face
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { this.rotationY = 180f },
                ) {
                    val cardWidth = maxWidth
                    val isSmall = cardWidth < Dimensions.Card.SmallCardThreshold
                    val cornerPadding = if (isSmall) 4.dp else 6.dp

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(cornerPadding)
                    ) {
                        CardCorner(
                            rank = card.rank.symbol,
                            suit = card.suit.symbol,
                            color = card.suit.color,
                            isSmall = isSmall,
                            modifier = Modifier.align(Alignment.TopStart)
                        )

                        if (!isSmall) {
                            CardFace(
                                rank = card.rank,
                                suit = card.suit,
                                modifier = Modifier.fillMaxSize()
                            )

                            CardCorner(
                                rank = card.rank.symbol,
                                suit = card.suit.symbol,
                                color = card.suit.color,
                                isSmall = false,
                                modifier =
                                    Modifier
                                        .align(Alignment.BottomEnd)
                                        .rotate(180f)
                            )
                        }
                    }
                }
            } else {
                // Back
                CardBack(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
