package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun InsuranceOverlay(
    onInsure: () -> Unit,
    onDecline: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(GlassDark)
                .windowInsetsPadding(safeDrawingInsets()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, GlassLight, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "INSURANCE?",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dealer shows an ACE. Insurance pays 2:1.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CasinoButton(
                    text = "NO THANKS",
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                )
                CasinoButton(
                    text = "INSURE",
                    onClick = onInsure,
                    modifier = Modifier.weight(1f),
                    isStrategic = true,
                )
            }
        }
    }
}
