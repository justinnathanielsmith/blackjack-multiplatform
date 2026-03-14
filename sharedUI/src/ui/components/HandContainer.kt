package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

@Composable
fun HandContainer(
    title: String,
    score: Int,
    bet: Int? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val borderColor =
        if (isActive) {
            PrimaryGold
        } else if (isPending) {
            GlassLight
        } else {
            Color.White.copy(alpha = 0.05f)
        }
    val backgroundColor =
        if (isActive) {
            PrimaryGold.copy(alpha = 0.1f)
        } else if (isPending) {
            Color.Black.copy(alpha = 0.2f)
        } else {
            GlassDark.copy(alpha = 0.3f)
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(vertical = 20.dp, horizontal = 16.dp),
    ) {
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .background(PrimaryGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "ACTIVE",
                    color = BackgroundDark,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        } else if (isPending) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "PENDING",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 24.dp),
                ) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Black,
                    )
                    if (bet != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BetChip(amount = bet, isActive = isActive)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) PrimaryGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            )
        }
    }
}
