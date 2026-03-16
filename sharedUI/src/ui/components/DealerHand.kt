package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.smithjustinn.blackjack.Hand

@Composable
fun DealerHand(
    hand: Hand,
    score: Int,
    title: String,
    isCompact: Boolean = false,
    isExtraCompact: Boolean = false,
    isSlowReveal: Boolean = false,
    modifier: Modifier = Modifier,
) {
    BlackjackHandContainer(
        title = title,
        score = score,
        isCompact = isCompact,
        isExtraCompact = isExtraCompact,
        isDealer = true,
        modifier = modifier,
    ) {
        HandRow(
            hand = hand,
            isDealer = true,
            isCompact = isCompact || isExtraCompact,
            isSlowReveal = isSlowReveal,
            scale = if (isCompact || isExtraCompact) 0.82f else 1f,
        )
    }
}
