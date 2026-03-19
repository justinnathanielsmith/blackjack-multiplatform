package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.result_loss
import sharedui.generated.resources.result_push
import sharedui.generated.resources.result_win

enum class HandResult {
    NONE,
    WIN,
    LOSS,
    PUSH
}

enum class HandStatus {
    ACTIVE,
    WAITING,
    BUSTED
}

@Composable
internal fun HandOutcomeBadge(
    result: HandResult,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        when (result) {
            HandResult.WIN -> PrimaryGold
            HandResult.LOSS -> TacticalRed
            HandResult.PUSH -> Color(0xFF555555)
            HandResult.NONE -> Color.Transparent
        }
    val contentColor =
        when (result) {
            HandResult.WIN -> BackgroundDark
            else -> Color.White
        }
    val text =
        when (result) {
            HandResult.WIN -> stringResource(Res.string.result_win)
            HandResult.LOSS -> stringResource(Res.string.result_loss)
            HandResult.PUSH -> stringResource(Res.string.result_push)
            HandResult.NONE -> ""
        }

    AnimatedVisibility(
        visible = result != HandResult.NONE,
        enter = scaleIn(spring(dampingRatio = 0.5f, stiffness = 400f)) + fadeIn(tween(200)),
        exit = scaleOut(tween(150)) + fadeOut(tween(150)),
        modifier = modifier,
    ) {
        Box(
            modifier =
                Modifier
                    .drawWithCache {
                        val glowBrush =
                            Brush.radialGradient(
                                colors = listOf(containerColor.copy(alpha = 0.4f), Color.Transparent),
                                radius = size.maxDimension * 1.2f,
                            )
                        onDrawBehind { drawCircle(brush = glowBrush) }
                    }.shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color.Black, ambientColor = Color.Black)
                    .background(containerColor, RoundedCornerShape(12.dp))
                    .border(
                        width = 2.dp,
                        color =
                            if (result ==
                                HandResult.WIN
                            ) {
                                BackgroundDark.copy(alpha = 0.3f)
                            } else {
                                Color.White.copy(alpha = 0.4f)
                            },
                        shape = RoundedCornerShape(12.dp),
                    ).padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                color = contentColor,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                fontSize = 24.sp,
                letterSpacing = 4.sp,
            )
        }
    }
}
