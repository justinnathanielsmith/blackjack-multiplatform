package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.emoji_crown

@Composable
internal fun HudTitleBadge(
    title: String,
    isDealer: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when {
            isDealer -> BackgroundDark.copy(alpha = 0.85f)
            isActive -> PrimaryGold.copy(alpha = 0.95f)
            else -> Color(0xCC2A2A2A)
        }
    val contentColor =
        when {
            isDealer -> PrimaryGold
            isActive -> BackgroundDark
            else -> Color.White.copy(alpha = 0.9f)
        }
    val borderColor =
        when {
            isDealer -> PrimaryGold.copy(alpha = 0.4f)
            isActive -> Color.White.copy(alpha = 0.5f)
            else -> Color.White.copy(alpha = 0.15f)
        }

    Row(
        modifier =
            modifier
                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                .background(containerColor, RoundedCornerShape(12.dp))
                .border(0.5.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isDealer) {
            Text(
                text = stringResource(Res.string.emoji_crown),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = contentColor,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
    }
}
