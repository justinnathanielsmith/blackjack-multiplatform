package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.BadgeDarkInner
import io.github.smithjustinn.blackjack.ui.theme.BadgeNeutralGrey
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.LeatherBlack
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldDark
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.VelvetRed
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.score_accessibility_blackjack
import sharedui.generated.resources.score_accessibility_bust
import sharedui.generated.resources.score_accessibility_dealer
import sharedui.generated.resources.score_accessibility_generic

enum class ScoreBadgeState {
    ACTIVE,
    DEALER,
    WAITING
}

// Sleek pill shape
val BadgeShape = RoundedCornerShape(percent = 50)

@Composable
fun ScoreBadge(
    score: Int,
    state: ScoreBadgeState,
    modifier: Modifier = Modifier,
    label: String? = null,
    isWinner: Boolean = false
) {
    val isBust = score > 21
    val is21 = score == 21

    val backgroundColor =
        when {
            isBust -> VelvetRed
            is21 || isWinner -> ModernGoldLight
            state == ScoreBadgeState.ACTIVE -> BadgeNeutralGrey // Neutral for live hands
            state == ScoreBadgeState.DEALER -> BackgroundDark
            else -> BadgeDarkInner // Deep dark for waiting
        }

    val borderColor =
        when {
            isBust -> Color.White.copy(alpha = 0.6f)
            is21 || isWinner -> Color.White.copy(alpha = 0.5f)
            state == ScoreBadgeState.ACTIVE -> ModernGoldLight.copy(alpha = 0.4f)
            state == ScoreBadgeState.DEALER -> ModernGoldLight
            else -> Color.White.copy(alpha = 0.15f)
        }

    val textColor =
        when {
            isBust -> Color.White
            is21 || isWinner -> LeatherBlack
            state == ScoreBadgeState.ACTIVE -> Color.White
            state == ScoreBadgeState.DEALER -> ModernGoldLight
            else -> Color.White.copy(alpha = 0.7f)
        }

    val announcement =
        when {
            isBust -> stringResource(Res.string.score_accessibility_bust, score)
            is21 -> stringResource(Res.string.score_accessibility_blackjack)
            state == ScoreBadgeState.DEALER -> stringResource(Res.string.score_accessibility_dealer, score)
            else -> stringResource(Res.string.score_accessibility_generic, score)
        }

    AnimatedVisibility(
        visible = score > 0,
        enter =
            scaleIn(
                animationSpec =
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
            ) + fadeIn(),
        exit = scaleOut() + fadeOut(),
        modifier = modifier
    ) {
        val pulseScale = remember { Animatable(1f) }
        val goldGlowAlpha = remember { Animatable(0f) }

        LaunchedEffect(score, state) {
            if (state == ScoreBadgeState.ACTIVE || is21 || isBust || isWinner) {
                launch {
                    pulseScale.animateTo(
                        targetValue = 1.25f,
                        animationSpec =
                            spring(
                                dampingRatio = 0.5f,
                                stiffness = Spring.StiffnessHigh
                            )
                    )
                    pulseScale.animateTo(
                        targetValue = 1f,
                        animationSpec =
                            spring(
                                dampingRatio = 0.6f,
                                stiffness = Spring.StiffnessMedium
                            )
                    )
                }
                if (state == ScoreBadgeState.ACTIVE) {
                    goldGlowAlpha.animateTo(0.7f, animationSpec = tween(80))
                    goldGlowAlpha.animateTo(0f, animationSpec = tween(250))
                }
            }
        }

        Box(
            modifier =
                Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = announcement
                    }.graphicsLayer {
                        scaleX = pulseScale.value
                        scaleY = pulseScale.value
                    }.shadow(
                        elevation =
                            if (state == ScoreBadgeState.ACTIVE ||
                                state == ScoreBadgeState.DEALER ||
                                is21 ||
                                isBust ||
                                isWinner
                            ) {
                                16.dp
                            } else {
                                4.dp
                            },
                        shape = BadgeShape,
                        spotColor =
                            if (state ==
                                ScoreBadgeState.ACTIVE
                            ) {
                                ModernGoldLight
                            } else {
                                backgroundColor
                            }
                    ).drawWithCache {
                        val glowRadius = size.maxDimension * 1.5f * pulseScale.value
                        val glowBrush =
                            Brush.radialGradient(
                                colors = listOf(backgroundColor.copy(alpha = 0.4f), Color.Transparent),
                                radius = glowRadius
                            )
                        onDrawBehind {
                            if (state == ScoreBadgeState.ACTIVE || is21 || isBust || isWinner) {
                                drawRect(glowBrush)
                            }
                            if (state == ScoreBadgeState.ACTIVE && goldGlowAlpha.value > 0f) {
                                drawRect(
                                    brush =
                                        Brush.radialGradient(
                                            colors =
                                                listOf(
                                                    ModernGoldLight.copy(alpha = goldGlowAlpha.value),
                                                    Color.Transparent
                                                ),
                                            center = Offset(size.width / 2f, size.height / 2f),
                                            radius = size.maxDimension * 1.4f
                                        )
                                )
                            }
                        }
                    }.then(
                        if (state == ScoreBadgeState.ACTIVE) {
                            Modifier.border(
                                1.5.dp,
                                Brush.verticalGradient(
                                    listOf(Color.White.copy(alpha = 0.5f), ModernGoldLight.copy(alpha = 0.5f))
                                ),
                                BadgeShape
                            )
                        } else {
                            Modifier
                        }
                    ).drawWithCache {
                        val isGold = is21 || isWinner
                        val brush =
                            if (isGold) {
                                Brush.verticalGradient(listOf(ModernGoldLight, ModernGoldDark))
                            } else {
                                Brush.verticalGradient(listOf(backgroundColor.copy(alpha = 0.95f), backgroundColor))
                            }

                        onDrawBehind {
                            drawRoundRect(
                                brush = brush,
                                cornerRadius = CornerRadius(size.height / 2f)
                            )

                            // Subtle Leather Grain / Micro-texture
                            val grainAlpha = 0.04f
                            for (i in 0..15) {
                                drawCircle(
                                    color = Color.Black.copy(alpha = grainAlpha),
                                    radius = 1.dp.toPx(),
                                    center =
                                        Offset(
                                            x = (size.width * (i * 0.13f % 1f)),
                                            y = (size.height * (i * 0.27f % 1f))
                                        )
                                )
                            }
                        }
                    }.border(1.dp, borderColor.copy(alpha = 0.5f), BadgeShape)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (label != null) {
                    Text(
                        text = label.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Black,
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                AnimatedContent(
                    targetState = score,
                    transitionSpec = {
                        val springSpec =
                            spring<Float>(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        (scaleIn(animationSpec = springSpec) + fadeIn())
                            .togetherWith(scaleOut() + fadeOut())
                    },
                    label = "scoreRoll"
                ) { targetScore ->
                    Text(
                        text = targetScore.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = textColor,
                        fontWeight = FontWeight.Black,
                    )
                }
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ScoreBadgePreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 18, state = ScoreBadgeState.ACTIVE)
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ScoreBadgeBustPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 23, state = ScoreBadgeState.ACTIVE)
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun ScoreBadgeTwentyOnePreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ScoreBadge(score = 21, state = ScoreBadgeState.ACTIVE)
        }
    }
}
