package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.components.chips.ChipStack
import io.github.smithjustinn.blackjack.ui.components.feedback.BetAmountBadge
import io.github.smithjustinn.blackjack.ui.components.layout.flightProgress

@Composable
internal fun PositionedChipItem(
    amount: Int,
    coordOffsetX: Float,
    coordOffsetY: Float,
    density: Density,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    handIndex: Int = 0,
    handCount: Int = 1,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(amount) {
        val zeta = 0.65f
        val stiffness = Spring.StiffnessMedium
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = zeta, stiffness = stiffness),
        )
    }

    Box(
        modifier =
            modifier
                .flightProgress(progress.asState()),
        contentAlignment = Alignment.Center,
    ) {
        ChipStack(amount = amount, isActive = isActive)

        // Bet Label overlay — positioned at the base of the chips
        BetAmountBadge(
            amount = amount,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        translationY = 12.dp.toPx()
                    }
        )
    }
}
