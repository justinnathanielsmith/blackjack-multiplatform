package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun BetChip(
    amount: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.2f)
    val backgroundColor = if (isActive) PrimaryGold else PrimaryGold.copy(alpha = 0.4f)
    val textColor = if (isActive) BackgroundDark else BackgroundDark.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .background(backgroundColor, RoundedCornerShape(24.dp))
                    .border(2.dp, borderColor, RoundedCornerShape(24.dp))
                    .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .border(2.dp, BackgroundDark.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$$amount",
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                )
            }
        }
    }
}
