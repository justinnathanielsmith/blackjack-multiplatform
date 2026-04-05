package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.DeepWine
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.net_result_lost
import sharedui.generated.resources.net_result_push
import sharedui.generated.resources.net_result_won
import sharedui.generated.resources.status_announcement_template
import sharedui.generated.resources.status_blackjack_exclamation
import sharedui.generated.resources.status_bust
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_dealing
import sharedui.generated.resources.status_player_won

@Composable
fun GameStatusMessage(
    status: GameStatus,
    modifier: Modifier = Modifier,
    netPayout: Int? = null,
    isCompact: Boolean = false,
    isBlackjack: Boolean = false,
    isBust: Boolean = false,
) {
    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by
        pulseTransition.animateFloat(
            initialValue = 0.98f,
            targetValue = if (isBlackjack) 1.15f else 1.04f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        tween(
                            durationMillis =
                                if (isBlackjack) {
                                    AnimationConstants.PulseDurationBlackjack
                                } else {
                                    AnimationConstants.PulseDurationNormal
                                },
                            easing = FastOutSlowInEasing,
                        ),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "pulseScale",
        )

    val shimmerTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerX by shimmerTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis =
                            if (isBlackjack) {
                                AnimationConstants.ShimmerDurationBlackjack
                            } else {
                                AnimationConstants.ShimmerDurationNormal
                            },
                        easing = LinearEasing,
                        delayMillis =
                            if (isBlackjack) {
                                AnimationConstants.ShimmerDelayBlackjack
                            } else {
                                AnimationConstants.ShimmerDelayNormal
                            },
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmerX",
    )

    val ring1Radius = remember { Animatable(0f) }
    val ring1Alpha = remember { Animatable(0f) }
    val ring2Radius = remember { Animatable(0f) }
    val ring2Alpha = remember { Animatable(0f) }
    val ring3Radius = remember { Animatable(0f) }
    val ring3Alpha = remember { Animatable(0f) }

    val borderRotationTransition = rememberInfiniteTransition(label = "borderRotation")
    val borderRotation by borderRotationTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(AnimationConstants.BorderRotationDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "borderRotation",
    )

    val reflectionTransition = rememberInfiniteTransition(label = "reflection")
    val reflectionX by reflectionTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = AnimationConstants.GlassReflectionDuration,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "reflectionX",
    )

    // Count-up animation for net payout
    val animatedPayout by animateIntAsState(
        targetValue = netPayout ?: 0,
        animationSpec = tween(durationMillis = AnimationConstants.PayoutCountUpDuration, easing = FastOutSlowInEasing),
        label = "animatedPayout",
    )

    LaunchedEffect(status, isBlackjack) {
        if (status == GameStatus.PLAYER_WON) {
            ring1Radius.snapTo(0f)
            ring1Alpha.snapTo(1f)
            ring2Radius.snapTo(0f)
            ring2Alpha.snapTo(1f)
            ring3Radius.snapTo(0f)
            ring3Alpha.snapTo(1f)
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
            launch {
                kotlinx.coroutines.delay(AnimationConstants.RingExpandDelay2.toLong())
                ring3Radius.animateTo(1f, tween(AnimationConstants.RingExpandDuration))
            }
            launch {
                kotlinx.coroutines.delay(AnimationConstants.RingExpandDelay2.toLong())
                ring3Alpha.animateTo(0f, tween(AnimationConstants.RingExpandDuration))
            }
        }
    }

    val statusText =
        when {
            isBlackjack -> stringResource(Res.string.status_blackjack_exclamation)
            isBust -> stringResource(Res.string.status_bust)
            status == GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            status == GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            else -> ""
        }

    val accentColor =
        when (status) {
            GameStatus.PLAYER_WON -> ModernGoldLight
            GameStatus.DEALER_WON -> TacticalRed
            GameStatus.PUSH -> Color.White
            else -> ModernGoldLight.copy(alpha = 0.8f)
        }

    val bannerBackgroundTopColor =
        when {
            isBlackjack -> PrimaryGold.copy(alpha = 0.15f)
            status == GameStatus.PLAYER_WON -> FeltGreen.copy(alpha = 0.85f)
            status == GameStatus.DEALER_WON -> DeepWine.copy(alpha = 0.85f)
            status == GameStatus.PUSH -> Color.Gray.copy(alpha = 0.85f)
            else -> DeepWine.copy(alpha = 0.85f)
        }

    // Delegate to the authoritative domain extension — consistent with all other sharedUI consumers.
    val isTerminal = status.isTerminal()

    val netLabel: String? =
        if (isTerminal && netPayout != null) {
            when {
                netPayout > 0 -> stringResource(Res.string.net_result_won, netPayout)
                netPayout < 0 -> stringResource(Res.string.net_result_lost, -netPayout)
                else -> stringResource(Res.string.net_result_push)
            }
        } else {
            null
        }

    val screenReaderAnnouncement =
        if (netLabel != null) {
            stringResource(Res.string.status_announcement_template, statusText, netLabel)
        } else {
            statusText
        }

    Box(
        modifier =
            modifier
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = screenReaderAnnouncement
                }.graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }.drawWithCache {
                    val borderBrush =
                        Brush.sweepGradient(
                            colors =
                                if (isTerminal) {
                                    listOf(
                                        accentColor.copy(alpha = 0.1f),
                                        accentColor,
                                        accentColor.copy(alpha = 0.1f)
                                    )
                                } else {
                                    listOf(ModernGoldDark, ModernGoldLight, ModernGoldDark)
                                }
                        )

                    onDrawBehind {
                        // 1. Ambient Glow
                        drawCircle(
                            brush =
                                Brush.radialGradient(
                                    colors = listOf(accentColor.copy(alpha = 0.2f), Color.Transparent),
                                    radius = size.maxDimension * 0.8f
                                ),
                            radius = size.maxDimension * 0.8f
                        )

                        // 2. Win Rings
                        if (status == GameStatus.PLAYER_WON) {
                            drawCircle(
                                color = accentColor.copy(alpha = ring1Alpha.value * 0.65f),
                                radius = ring1Radius.value * size.maxDimension * 0.7f,
                                style = Stroke(3.dp.toPx()),
                            )
                            drawCircle(
                                color = accentColor.copy(alpha = ring2Alpha.value * 0.5f),
                                radius = ring2Radius.value * size.maxDimension * 0.7f,
                                style = Stroke(2.dp.toPx()),
                            )
                        }

                        // 3. Rotating Border
                        rotate(borderRotation) {
                            drawRoundRect(
                                brush = borderBrush,
                                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                style = Stroke(2.dp.toPx()),
                                alpha = 0.8f
                            )
                        }
                    }
                }.clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.75f)) // Base glass dark
                .background(
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                bannerBackgroundTopColor,
                                Color.Black.copy(alpha = 0.4f)
                            )
                    )
                ).border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp)
                ).padding(horizontal = 56.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        // Reflection overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val reflectionWidth = size.width * 0.4f
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors =
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.05f),
                                Color.White.copy(alpha = 0.12f),
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                        start = Offset(size.width * reflectionX - reflectionWidth / 2f, 0f),
                        end = Offset(size.width * reflectionX + reflectionWidth / 2f, size.height)
                    ),
                blendMode = BlendMode.Overlay
            )
        }
        val netLabelColor =
            when {
                netPayout != null && netPayout > 0 -> PrimaryGold
                netPayout != null && netPayout < 0 -> TacticalRed
                else -> Color.White.copy(alpha = 0.7f)
            }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusText.uppercase(),
                style =
                    if (isCompact) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.displayMedium
                    }.copy(letterSpacing = 4.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier.drawWithCache {
                        // Brush is created once per size/accentColor change — shimmerX is
                        // intentionally NOT read here, so the cache is NOT invalidated on
                        // every animation frame (~60fps). This eliminates per-frame Brush,
                        // List<Color>, and Offset allocations during win/push celebrations.
                        val bandWidth = size.width * 0.3f
                        val shimmerBrush =
                            Brush.linearGradient(
                                colors =
                                    listOf(
                                        Color.Transparent,
                                        accentColor.copy(alpha = 0.3f),
                                        accentColor,
                                        accentColor.copy(alpha = 0.3f),
                                        Color.Transparent
                                    ),
                                start = Offset.Zero,
                                end = Offset(bandWidth, size.height)
                            )
                        onDrawWithContent {
                            drawContent()
                            if (status == GameStatus.PLAYER_WON || status == GameStatus.PUSH) {
                                // shimmerX read here: plain Float math, no allocation.
                                // The band is centred on shimmerX * size.width and drawn 2× bandWidth
                                // wide so the gradient fades are visible on both edges.
                                drawRect(
                                    brush = shimmerBrush,
                                    topLeft = Offset(size.width * shimmerX - bandWidth / 2f, 0f),
                                    size = Size(bandWidth * 2f, size.height),
                                    blendMode = BlendMode.SrcAtop
                                )
                            }
                        }
                    },
            )
            if (netLabel != null) {
                Text(
                    text =
                        if (animatedPayout > 0) {
                            stringResource(Res.string.net_result_won, animatedPayout)
                        } else if (animatedPayout < 0) {
                            stringResource(Res.string.net_result_lost, -animatedPayout)
                        } else {
                            stringResource(Res.string.net_result_push)
                        },
                    color = netLabelColor,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun GameStatusToast(
    status: GameStatus,
    modifier: Modifier = Modifier,
) {
    val dotTransition = rememberInfiniteTransition(label = "toastDot")
    val dotAlpha by dotTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        durationMillis = AnimationConstants.PulseDurationNormal,
                        easing = FastOutSlowInEasing,
                    ),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "dotAlpha",
    )

    val statusText =
        when (status) {
            GameStatus.DEALING -> stringResource(Res.string.status_dealing)
            GameStatus.DEALER_TURN -> stringResource(Res.string.status_dealer_turn)
            else -> ""
        }

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(48.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = statusText
                }.background(Color.Black.copy(alpha = 0.75f))
                .drawWithCache {
                    onDrawBehind {
                        drawLine(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            ModernGoldDark,
                                            ModernGoldLight,
                                            ModernGoldDark,
                                            Color.Transparent,
                                        )
                                ),
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.5.dp.toPx(),
                        )
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(7.dp)
                        .graphicsLayer { alpha = dotAlpha }
                        .clip(CircleShape)
                        .background(ModernGoldLight),
            )
            Text(
                text = statusText.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 3.sp),
                color = Color.White.copy(alpha = 0.90f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusToastDealingPreview() {
    BlackjackTheme {
        GameStatusToast(status = GameStatus.DEALING)
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusToastDealerTurnPreview() {
    BlackjackTheme {
        GameStatusToast(status = GameStatus.DEALER_TURN)
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusMessagePlayerWonPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            GameStatusMessage(status = GameStatus.PLAYER_WON, netPayout = 200)
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusMessageBlackjackPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            GameStatusMessage(status = GameStatus.PLAYER_WON, netPayout = 300, isBlackjack = true)
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusMessageDealerWonPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            GameStatusMessage(status = GameStatus.DEALER_WON, netPayout = -100)
        }
    }
}

@Preview
@Composable
@Suppress("UnusedPrivateMember") // Used by Compose Preview
private fun GameStatusMessagePushPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(32.dp)) {
            GameStatusMessage(status = GameStatus.PUSH, netPayout = 0)
        }
    }
}
