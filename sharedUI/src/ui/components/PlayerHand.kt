package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Hand
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PlayerHand(
    handTotal: Int,
    status: HandStatus,
    cards: List<Card>,
    result: HandResult = HandResult.NONE,
    title: String? = null,
    isCompact: Boolean = false,
    isExtraCompact: Boolean = false,
    showStatus: Boolean = true,
    onPositioned: ((Offset) -> Unit)? = null,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val isActive = status == HandStatus.ACTIVE
    val isWaiting = status == HandStatus.WAITING

    val alpha = if (isWaiting) 0.5f else 1.0f
    val baseScale =
        when {
            cards.size >= 7 -> 0.75f
            cards.size >= 5 -> 0.85f
            else -> 1.0f
        }

    val targetScale =
        when {
            isActive -> 1.05f * baseScale
            isWaiting -> 0.9f * baseScale
            else -> 1.0f * baseScale
        }
    val animatedScale by animateFloatAsState(targetScale, label = "handScale")
    val scaleModifier = Modifier.scale(animatedScale)

    BlackjackHandContainer(
        score = handTotal,
        title = title,
        isActive = isActive,
        isPending = isWaiting,
        result = result,
        isCompact = isCompact,
        isExtraCompact = isExtraCompact,
        showStatus = showStatus,
        onPositioned = onPositioned,
        modifier =
            modifier
                .then(scaleModifier)
                .alpha(alpha)
    ) {
        HandRow(
            hand = Hand(cards.toPersistentList()),
            isCompact = isCompact || isExtraCompact,
            scale = scale
        )
    }
}
