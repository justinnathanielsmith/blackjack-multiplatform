package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import kotlinx.coroutines.launch

data class PayoutEvent(
    val id: Long,
    val amount: Int,
    val isWin: Boolean = true,
)

/**
 * A container meant to be placed over the table to render floating
 * text or flying chips when payouts happen.
 */
@Composable
fun PayoutAnimationsOverlay(
    events: List<PayoutEvent>,
    onEventFinished: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        events.forEach { event ->
            PayoutToast(event = event, onFinished = { onEventFinished(event.id) })
        }
    }
}

@Composable
private fun PayoutToast(
    event: PayoutEvent,
    onFinished: () -> Unit
) {
    val offsetY = remember { Animatable(50f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(event.id) {
        launch {
            offsetY.animateTo(
                targetValue = -100f,
                animationSpec =
                    tween(
                        durationMillis = AnimationConstants.PayoutSlideUpDuration,
                        easing = FastOutSlowInEasing
                    )
            )
        }
        launch {
            alphaAnim.animateTo(1f, tween(AnimationConstants.PayoutFadeDuration))
            kotlinx.coroutines.delay(AnimationConstants.PayoutHoldDuration)
            alphaAnim.animateTo(0f, tween(AnimationConstants.PayoutFadeDuration))
            onFinished()
        }
    }

    val color = if (event.isWin) PrimaryGold else TacticalRed
    val prefix = if (event.isWin) "+" else "-"

    Text(
        text = "$prefix$${event.amount}",
        style = MaterialTheme.typography.displayMedium,
        color = color,
        fontWeight = FontWeight.Black,
        modifier =
            Modifier
                .graphicsLayer {
                    translationY = offsetY.value.dp.toPx()
                    this.alpha = alphaAnim.value
                }
    )
}
