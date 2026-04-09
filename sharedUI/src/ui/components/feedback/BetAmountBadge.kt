package io.github.smithjustinn.blackjack.ui.components.feedback

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.LeatherBlack
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.currency_template

@Composable
fun BetAmountBadge(
    amount: Int,
    modifier: Modifier = Modifier
) {
    val scale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec =
                spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
        )
    }

    Box(
        modifier =
            modifier
                .scale(scale.value)
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(4.dp),
                    spotColor = ModernGoldDark.copy(alpha = 0.5f)
                ).background(LeatherBlack, RoundedCornerShape(4.dp))
                .border(1.dp, ModernGoldDark, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Delicate inner border line to create placard feeling
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .border(0.5.dp, ModernGoldLight.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
        )
        Text(
            text = stringResource(Res.string.currency_template, amount.toString()),
            color = ModernGoldLight,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}
