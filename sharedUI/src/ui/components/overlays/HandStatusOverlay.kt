package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_blackjack
import sharedui.generated.resources.status_bust
import sharedui.generated.resources.status_twenty_one

@Composable
internal fun HandStatusOverlay(
    // Domain predicates pre-computed at the call site — no phase-gating logic in Composables
    isBust: Boolean,
    isBlackjack: Boolean,
    isTwentyOne: Boolean,
    modifier: Modifier = Modifier,
) {
    val visible = isBust || isBlackjack || isTwentyOne

    AnimatedVisibility(
        visible = visible,
        enter =
            fadeIn(tween(AnimationConstants.StatusMessageEnterDuration)) +
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                ),
        exit =
            fadeOut(tween(AnimationConstants.StatusMessageExitDuration)) +
                scaleOut(targetScale = 0.9f),
        modifier = modifier
    ) {
        val (text, color) =
            when {
                isBust -> stringResource(Res.string.status_bust) to TacticalRed
                isBlackjack -> stringResource(Res.string.status_blackjack) to PrimaryGold
                else -> stringResource(Res.string.status_twenty_one) to PrimaryGold
            }

        Box(
            modifier =
                Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                    }.shadow(12.dp, RoundedCornerShape(8.dp))
                    .background(color, RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = if (isBust) Color.White else BackgroundDark,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp),
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
            )
        }
    }
}
