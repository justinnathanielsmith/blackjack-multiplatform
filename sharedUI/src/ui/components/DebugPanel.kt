package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.data.AppSettings

@Composable
fun DebugPanel(
    state: GameState,
    settings: AppSettings,
    onAction: (GameAction) -> Unit,
    onResetBalance: () -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(12.dp)
    ) {
        Text(
            text = "DEBUG PANEL",
            style = MaterialTheme.typography.labelSmall,
            color = Color.Yellow,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DebugItem("Status", state.status.name)
            DebugItem("Deck", "${state.deck.size}")
            DebugItem("ActiveIdx", "${state.activeHandIndex}")
            DebugItem("Hands", "${state.playerHands.size}")
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onAction(
                        GameAction.NewGame(rules = settings.gameRules, handCount = settings.defaultHandCount)
                    )
                },
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("RESTART (NEW RULES)", fontSize = 10.sp)
            }
            Button(
                onClick = onResetBalance,
                modifier = Modifier.height(32.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                shape = RoundedCornerShape(4.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B0000))
            ) {
                Text("RESET BALANCE", fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun DebugItem(
    label: String,
    value: String
) {
    Column {
        Text(text = label, fontSize = 9.sp, color = Color.Gray)
        Text(
            text = value,
            fontSize = 11.sp,
            color = Color.White,
            fontFamily = FontFamily.Monospace
        )
    }
}
