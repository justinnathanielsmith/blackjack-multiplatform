package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.Hand
import kotlinx.collections.immutable.toPersistentList

@Composable
fun PlayerHand(
    handTotal: Int,
    status: HandStatus,
    cards: List<Card>,
    bet: Int? = null,
    result: HandResult = HandResult.NONE,
    title: String? = null,
    isCompact: Boolean = false,
    isExtraCompact: Boolean = false,
    modifier: Modifier = Modifier,
    scale: Float = 1.0f
) {
    val isActive = status == HandStatus.ACTIVE
    val isWaiting = status == HandStatus.WAITING

    val alpha = if (isWaiting) 0.5f else 1.0f
    val scaleModifier = if (isWaiting) Modifier.scale(0.9f) else Modifier

    BlackjackHandContainer(
        score = handTotal,
        title = title,
        isActive = isActive,
        isPending = isWaiting,
        bet = bet,
        result = result,
        isCompact = isCompact,
        isExtraCompact = isExtraCompact,
        modifier = modifier
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
