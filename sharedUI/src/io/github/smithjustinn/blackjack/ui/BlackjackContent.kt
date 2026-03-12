package io.github.smithjustinn.blackjack.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand

@Composable
fun BlackjackContent(component: BlackjackComponent) {
    val state by component.state.collectAsState()

    val dealerScore = if (state.status == GameStatus.PLAYING && state.dealerHand.cards.isNotEmpty()) {
        "?"
    } else {
        state.dealerHand.score.toString()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Blackjack", style = MaterialTheme.typography.headlineLarge)

        Text("Dealer's Hand (Score: $dealerScore)")
        HandRow(state.dealerHand, hideFirstCard = state.status == GameStatus.PLAYING)

        Text("Player's Hand (Score: ${state.playerHand.score})")
        HandRow(state.playerHand, hideFirstCard = false)

        Text("Status: ${state.status}", style = MaterialTheme.typography.titleMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { component.onAction(GameAction.Hit) },
                enabled = state.status == GameStatus.PLAYING
            ) {
                Text("Hit")
            }
            Button(
                onClick = { component.onAction(GameAction.Stand) },
                enabled = state.status == GameStatus.PLAYING
            ) {
                Text("Stand")
            }
            Button(onClick = { component.onAction(GameAction.NewGame) }) {
                Text("New Game")
            }
        }
    }
}

@Composable
fun HandRow(hand: Hand, hideFirstCard: Boolean = false) {
    Row(horizontalArrangement = Arrangement.spacedBy((-30).dp)) {
        hand.cards.forEachIndexed { index, card ->
            val isFaceUp = !(hideFirstCard && index == 0)
            PlayingCard(
                card = card,
                isFaceUp = isFaceUp
            )
        }
    }
}
