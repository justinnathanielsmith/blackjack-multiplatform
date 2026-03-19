package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(name = "Waiting state")
@Composable
private fun PreviewScoreBadgeWaiting() {
    ScoreBadge(score = 14, state = ScoreBadgeState.WAITING)
}

@Preview(name = "Active state")
@Composable
private fun PreviewScoreBadgeActive() {
    ScoreBadge(score = 17, state = ScoreBadgeState.ACTIVE)
}

@Preview(name = "Dealer state")
@Composable
private fun PreviewScoreBadgeDealer() {
    ScoreBadge(score = 19, state = ScoreBadgeState.DEALER)
}

@Preview(name = "Score 21")
@Composable
private fun PreviewScoreBadge21() {
    ScoreBadge(score = 21, state = ScoreBadgeState.ACTIVE)
}

@Preview(name = "Bust state")
@Composable
private fun PreviewScoreBadgeBust() {
    ScoreBadge(score = 23, state = ScoreBadgeState.ACTIVE)
}
