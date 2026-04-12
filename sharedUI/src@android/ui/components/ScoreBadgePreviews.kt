package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadge
import io.github.smithjustinn.blackjack.ui.components.feedback.ScoreBadgeState

@Preview(name = "Waiting state")
@Composable
internal fun PreviewScoreBadgeWaiting() {
    ScoreBadge(score = 14, isBust = false, is21 = false, state = ScoreBadgeState.WAITING)
}

@Preview(name = "Active state")
@Composable
internal fun PreviewScoreBadgeActive() {
    ScoreBadge(score = 17, isBust = false, is21 = false, state = ScoreBadgeState.ACTIVE)
}

@Preview(name = "Dealer state")
@Composable
internal fun PreviewScoreBadgeDealer() {
    ScoreBadge(score = 19, isBust = false, is21 = false, state = ScoreBadgeState.DEALER)
}

@Preview(name = "Score 21")
@Composable
internal fun PreviewScoreBadge21() {
    ScoreBadge(score = 21, isBust = false, is21 = true, state = ScoreBadgeState.ACTIVE)
}

@Preview(name = "Bust state")
@Composable
internal fun PreviewScoreBadgeBust() {
    ScoreBadge(score = 23, isBust = true, is21 = false, state = ScoreBadgeState.ACTIVE)
}
