package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.big_win_banner_text

@Composable
fun BigWinBanner(
    visible: Boolean,
    amount: Int,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        enter =
            scaleIn(
                initialScale = 0.4f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 380f),
            ) + fadeIn(tween(200)),
        exit =
            scaleOut(targetScale = 0.85f, animationSpec = tween(300)) +
                fadeOut(tween(300)),
        modifier = modifier,
    ) {
        BigWinBannerContent(amount = amount)
    }
}

@Composable
private fun BigWinBannerContent(
    amount: Int,
    modifier: Modifier = Modifier,
) {
    val shimmerTransition = rememberInfiniteTransition(label = "bigWinShimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700, easing = LinearEasing, delayMillis = 150),
                repeatMode = RepeatMode.Restart,
            ),
        label = "bigWinShimmerX",
    )

    val pulseTransition = rememberInfiniteTransition(label = "bigWinPulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.08f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(350, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "bigWinPulse",
    )

    val ring1Radius = remember { Animatable(0f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Radius = remember { Animatable(0f) }
    val ring2Alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        ring1Alpha.snapTo(1f)
        ring2Alpha.snapTo(1f)
        launch { ring1Radius.animateTo(1f, tween(AnimationConstants.RingExpandDuration)) }
        launch { ring1Alpha.animateTo(0f, tween(AnimationConstants.RingExpandDuration)) }
        launch {
            kotlinx.coroutines.delay(AnimationConstants.RingExpandDelay1.toLong())
            ring2Radius.animateTo(1f, tween(AnimationConstants.RingExpandDuration))
        }
        launch {
            kotlinx.coroutines.delay(AnimationConstants.RingExpandDelay1.toLong())
            ring2Alpha.animateTo(0f, tween(AnimationConstants.RingExpandDuration))
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }.drawWithCache {
                    val glowBrush =
                        Brush.radialGradient(
                            colors = listOf(PrimaryGold.copy(alpha = 0.35f), Color.Transparent),
                            radius = size.maxDimension * 1.1f,
                        )
                    onDrawBehind {
                        drawCircle(
                            color = PrimaryGold.copy(alpha = ring1Alpha.value * 0.65f),
                            radius = ring1Radius.value * size.maxDimension * 0.8f,
                            style = Stroke(3.dp.toPx()),
                        )
                        drawCircle(
                            color = ModernGoldLight.copy(alpha = ring2Alpha.value * 0.45f),
                            radius = ring2Radius.value * size.maxDimension * 0.8f,
                            style = Stroke(2.dp.toPx()),
                        )
                        drawRoundRect(
                            brush = glowBrush,
                            cornerRadius = CornerRadius(20.dp.toPx()),
                        )
                    }
                }.clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                Color(0xFF3A2000),
                                Color(0xFF1A0E00),
                            ),
                    ),
                ).border(
                    width = 2.5.dp,
                    brush =
                        Brush.horizontalGradient(
                            colors =
                                listOf(
                                    ModernGoldDark,
                                    PrimaryGold,
                                    ModernGoldLight,
                                    PrimaryGold,
                                    ModernGoldDark,
                                ),
                        ),
                    shape = RoundedCornerShape(16.dp),
                ).padding(horizontal = 56.dp, vertical = 28.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(Res.string.big_win_banner_text),
                style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 6.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.drawWithCache {
                        // Cache only the static color stops — shimmerX must NOT be read here.
                        // Reading an animated Float in the cache scope invalidates the cache every
                        // frame (~60×/s), defeating the purpose of drawWithCache.
                        val shimmerColors =
                            listOf(
                                Color.Transparent,
                                PrimaryGold.copy(alpha = 0.4f),
                                ModernGoldLight,
                                PrimaryGold.copy(alpha = 0.4f),
                                Color.Transparent,
                            )
                        val bandWidth = size.width * 0.35f
                        val shimmerBrush =
                            Brush.linearGradient(
                                colors = shimmerColors,
                                start = Offset.Zero,
                                end = Offset(bandWidth, size.height),
                            )
                        onDrawWithContent {
                            drawContent()
                            // shimmerX read here: plain Float math, no allocation.
                            translate(left = size.width * shimmerX) {
                                drawRect(
                                    brush = shimmerBrush,
                                    size = Size(bandWidth, size.height),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                    },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "+$amount",
                style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 2.sp),
                color = PrimaryGold,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
            )
        }
    }
}
