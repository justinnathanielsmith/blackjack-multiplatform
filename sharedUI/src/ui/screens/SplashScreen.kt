package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.presentation.SplashComponent
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.status_blackjack

@Composable
fun SplashScreen(component: SplashComponent) {
    val scale = remember { Animatable(0.5f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Animate in
        scale.animateTo(
            targetValue = 1f,
            animationSpec =
                tween(
                    durationMillis = AnimationConstants.SplashScaleInDuration,
                    easing = FastOutSlowInEasing
                )
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = AnimationConstants.SplashFadeInDuration)
        )

        // Hold for reading
        delay(AnimationConstants.SplashDisplayDurationMs)

        // Trigger finish
        component.onSplashFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // We'll use a premium looking text layout, maybe similar to a casino sign
        Text(
            text = stringResource(Res.string.status_blackjack),
            color = MaterialTheme.colorScheme.primary, // Gold
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        this.alpha = alpha.value
                    }
        )
    }
}
