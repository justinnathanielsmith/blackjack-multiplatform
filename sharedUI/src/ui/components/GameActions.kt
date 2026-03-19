package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.action_double
import sharedui.generated.resources.action_hit
import sharedui.generated.resources.action_split
import sharedui.generated.resources.action_stand
import sharedui.generated.resources.ic_double
import sharedui.generated.resources.ic_hit
import sharedui.generated.resources.ic_split
import sharedui.generated.resources.ic_stand

@Composable
fun GameActions(
    state: GameState,
    component: BlackjackComponent,
    isCompact: Boolean = false,
) {
    val audioService = LocalAppGraph.current.audioService

    val onHit =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.DEAL)
                component.onAction(GameAction.Hit)
            }
        }
    val onStand =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.Stand)
            }
        }
    val onDoubleDown =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.DEAL)
                component.onAction(GameAction.DoubleDown)
            }
        }
    val onSplit =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.DEAL)
                component.onAction(GameAction.Split)
            }
        }

    AnimatedContent(
        targetState = state.status,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
                fadeOut(animationSpec = tween(500))
        },
        label = "GameActionsTransition"
    ) { status ->
        val buttonHeight =
            if (isCompact) Dimensions.ActionBar.ButtonHeightCompact else Dimensions.ActionBar.ButtonHeightNormal

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .graphicsLayer { clip = false },
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = status == GameStatus.PLAYING,
                enter = fadeIn(tween(300)) + expandVertically(tween(300), expandFrom = Alignment.Top),
                exit = fadeOut(tween(300)) + shrinkVertically(tween(300), shrinkTowards = Alignment.Top),
            ) {
                val canSplit = state.canSplit()
                val canDouble = state.canDoubleDown()

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonModifier = Modifier.weight(1f).defaultMinSize(minHeight = buttonHeight)

                    ModernActionButton(
                        icon = Res.drawable.ic_double,
                        label = stringResource(Res.string.action_double),
                        onClick = onDoubleDown,
                        enabled = canDouble,
                        containerColor = PrimaryGold,
                        contentColor = BackgroundDark,
                        modifier = buttonModifier
                    )

                    if (canSplit) {
                        ModernActionButton(
                            icon = Res.drawable.ic_split,
                            label = stringResource(Res.string.action_split),
                            onClick = onSplit,
                            enabled = true,
                            containerColor = PrimaryGold,
                            contentColor = BackgroundDark,
                            modifier = buttonModifier
                        )
                    }

                    ModernActionButton(
                        icon = Res.drawable.ic_hit,
                        label = stringResource(Res.string.action_hit),
                        onClick = onHit,
                        enabled = true,
                        containerColor = GlassDark,
                        contentColor = ChipGreen,
                        borderColor = ChipGreen.copy(alpha = 0.5f),
                        modifier = buttonModifier
                    )

                    val activeHand = state.playerHands.getOrNull(state.activeHandIndex)
                    val tension = activeHand?.let { calculateTension(it.score) } ?: 0.0f

                    ModernActionButton(
                        icon = Res.drawable.ic_stand,
                        label = stringResource(Res.string.action_stand),
                        onClick = onStand,
                        enabled = true,
                        containerColor = GlassDark,
                        contentColor = TacticalRed,
                        borderColor = TacticalRed.copy(alpha = 0.5f),
                        modifier = buttonModifier,
                        tension = tension
                    )
                }
            }

            if (status != GameStatus.PLAYING && status != GameStatus.INSURANCE_OFFERED) {
                Spacer(modifier = Modifier.height(buttonHeight))
            } else if (status == GameStatus.INSURANCE_OFFERED) {
                Spacer(modifier = Modifier.height(buttonHeight))
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    borderColor: Color? = null,
    tension: Float = 0f,
) {
    val isGlowing = tension > 0f

    val glowModifier =
        if (isGlowing && enabled) {
            val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")

            // Faster pulsing at higher tension
            val pulseDuration = (1200 - (tension * 600)).toInt()

            val glowAlpha by infiniteTransition.animateFloat(
                initialValue = 0.4f * tension,
                targetValue = 0.9f * tension,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                label = "glowAlpha"
            )

            val glowScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.0f + (0.4f * tension),
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                label = "glowScale"
            )

            Modifier
                .drawBehind {
                    val extraGlow = 8.dp.toPx() * tension * glowScale
                    val glowSize = Size(size.width + extraGlow * 2, size.height + extraGlow * 2)
                    val glowTopLeft = Offset(-extraGlow, -extraGlow)

                    drawRoundRect(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(contentColor.copy(alpha = glowAlpha), Color.Transparent),
                                center = center,
                                radius = size.maxDimension * 0.8f * glowScale
                            ),
                        topLeft = glowTopLeft,
                        size = glowSize,
                        cornerRadius = CornerRadius(glowSize.height / 2)
                    )
                }.graphicsLayer {
                    // Subtle breathing scale for the whole button at high tension
                    if (tension > 0.8f) {
                        scaleX = 0.98f + (0.04f * glowScale)
                        scaleY = 0.98f + (0.04f * glowScale)
                    }
                    clip = false
                }
        } else {
            Modifier
        }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.then(glowModifier),
        shape = RoundedCornerShape(percent = 50),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor,
                disabledContainerColor = containerColor.copy(alpha = 0.3f),
                disabledContentColor = contentColor.copy(alpha = 0.3f)
            ),
        border =
            borderColor?.let {
                BorderStroke(1.dp, if (enabled) it else it.copy(alpha = 0.3f))
            },
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun calculateTension(score: Int): Float {
    return when {
        score >= 20 -> 1.0f
        score == 19 -> 0.7f
        score == 18 -> 0.4f
        score == 17 -> 0.2f
        else -> 0.0f
    }
}
