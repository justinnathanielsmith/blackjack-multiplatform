package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
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
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HandRow(
                hand = Hand(cards.toPersistentList()),
                isCompact = isCompact || isExtraCompact || scale < 0.9f,
                scale = scale * 0.85f
            )
        }
    }
}
