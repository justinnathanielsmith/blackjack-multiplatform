package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
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
import sharedui.generated.resources.action_surrender
import sharedui.generated.resources.ic_double
import sharedui.generated.resources.ic_hit
import sharedui.generated.resources.ic_split
import sharedui.generated.resources.ic_stand
import sharedui.generated.resources.ic_surrender

@Composable
fun GameActions(
    state: GameState,
    component: BlackjackComponent,
    isCompact: Boolean = false,
) {
    val onHit =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Hit)
            }
        }
    val onStand =
        remember(component) {
            {
                component.onPlayClick()
                component.onAction(GameAction.Stand)
            }
        }
    val onDoubleDown =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.DoubleDown)
            }
        }
    val onSplit =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Split)
            }
        }
    val onSurrender =
        remember(component) {
            {
                component.onPlayClick()
                component.onAction(GameAction.Surrender)
            }
        }

    AnimatedContent(
        targetState = state.status,
        transitionSpec = {
            fadeIn(animationSpec = tween(AnimationConstants.ActionStatusFadeDuration)) togetherWith
                fadeOut(animationSpec = tween(AnimationConstants.ActionStatusFadeDuration))
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
                enter =
                    fadeIn(tween(AnimationConstants.ActionPlayingSlideDuration)) +
                        expandVertically(
                            tween(AnimationConstants.ActionPlayingSlideDuration),
                            expandFrom = Alignment.Top
                        ),
                exit =
                    fadeOut(tween(AnimationConstants.ActionPlayingSlideDuration)) +
                        shrinkVertically(
                            tween(AnimationConstants.ActionPlayingSlideDuration),
                            shrinkTowards = Alignment.Top
                        ),
            ) {
                val canSplit = state.canSplit()
                val canDouble = state.canDoubleDown()
                val canSurrender = state.canSurrender()

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val buttonModifier = Modifier.weight(1f).defaultMinSize(minHeight = buttonHeight)

                    ModernActionButton(
                        icon = Res.drawable.ic_surrender,
                        label = stringResource(Res.string.action_surrender),
                        onClick = onSurrender,
                        enabled = canSurrender,
                        containerColor = GlassDark,
                        contentColor = Color.White,
                        borderColor = Color.White.copy(alpha = 0.5f),
                        modifier = buttonModifier
                    )

                    ModernActionButton(
                        icon = Res.drawable.ic_double,
                        label = stringResource(Res.string.action_double),
                        onClick = onDoubleDown,
                        enabled = canDouble,
                        containerColor = GlassDark,
                        contentColor = PrimaryGold,
                        borderColor = PrimaryGold.copy(alpha = 0.5f),
                        modifier = buttonModifier
                    )

                    ModernActionButton(
                        icon = Res.drawable.ic_split,
                        label = stringResource(Res.string.action_split),
                        onClick = onSplit,
                        enabled = canSplit,
                        containerColor = GlassDark,
                        contentColor = PrimaryGold,
                        borderColor = PrimaryGold.copy(alpha = 0.5f),
                        modifier = buttonModifier
                    )

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
                    val tension = activeHand?.tension ?: 0.0f

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

            if (status != GameStatus.PLAYING) {
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
    val isGlowing = tension > 0f && enabled
    val pulseDuration = if (isGlowing) (1200 - (tension * 600)).toInt() else 1200

    // Bolt Performance Optimization: State objects held without `by` delegate so their
    // `.value` reads are deferred to the drawBehind / graphicsLayer lambda (draw phase only).
    // Using `by` would subscribe the composable to every animation frame tick and cause
    // O(frames) full recompositions of all 4 ModernActionButton instances during gameplay.
    // This matches the pattern already used by ActiveHandGlow and HandZoneHud in OverlayCardTable.
    val infiniteTransition = rememberInfiniteTransition(label = "glowTransition")
    val glowAlphaState =
        infiniteTransition.animateFloat(
            initialValue = 0.4f * tension,
            targetValue = 0.9f * tension,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "glowAlpha"
        )
    val glowScaleState =
        infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.0f + (0.4f * tension),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(pulseDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
            label = "glowScale"
        )

    val glowModifier =
        if (isGlowing) {
            Modifier
                .drawBehind {
                    // Read .value here (draw phase) — no recomposition triggered.
                    val gScale = glowScaleState.value
                    val gAlpha = glowAlphaState.value
                    val extraGlow = 8.dp.toPx() * tension * gScale
                    val glowSize = Size(this.size.width + extraGlow * 2, this.size.height + extraGlow * 2)
                    val glowTopLeft = Offset(-extraGlow, -extraGlow)

                    this.drawRoundRect(
                        brush =
                            Brush.radialGradient(
                                colors = listOf(contentColor.copy(alpha = gAlpha), Color.Transparent),
                                center = center,
                                radius = this.size.maxDimension * 0.8f * gScale
                            ),
                        topLeft = glowTopLeft,
                        size = glowSize,
                        cornerRadius = CornerRadius(glowSize.height / 2)
                    )
                }.graphicsLayer {
                    // Read .value here (layer phase) — no recomposition triggered.
                    if (tension > 0.8f) {
                        val gScale = glowScaleState.value
                        scaleX = 0.98f + (0.04f * gScale)
                        scaleY = 0.98f + (0.04f * gScale)
                    }
                    clip = false
                }
        } else {
            Modifier
        }

    val buttonInteractionSource = remember { MutableInteractionSource() }
    val isPressed by buttonInteractionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (isPressed) 0.96f else 1f)

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            modifier
                .semantics {
                    role = Role.Button
                    contentDescription = label
                }.then(glowModifier)
                .graphicsLayer {
                    scaleX = buttonScale
                    scaleY = buttonScale
                },
        interactionSource = buttonInteractionSource,
        shape = RoundedCornerShape(percent = 50),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = Color.Transparent, // We draw our own background
                contentColor = contentColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = contentColor.copy(alpha = 0.3f)
            ),
        contentPadding = PaddingValues(0.dp) // Manual padding in content
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .background(
                        brush =
                            if (enabled) {
                                Brush.verticalGradient(
                                    0.0f to containerColor.copy(alpha = 0.9f),
                                    0.45f to containerColor,
                                    0.55f to containerColor.copy(alpha = 0.8f),
                                    1.0f to containerColor.copy(alpha = 0.7f)
                                )
                            } else {
                                Brush.verticalGradient(colors = listOf(GlassDark, GlassDark))
                            },
                        shape = RoundedCornerShape(percent = 50)
                    ).border(
                        width = 1.5.dp,
                        brush =
                            Brush.linearGradient(
                                0.0f to PrimaryGold.copy(alpha = 0.8f),
                                0.5f to PrimaryGold.copy(alpha = 0.2f),
                                1.0f to PrimaryGold.copy(alpha = 0.6f)
                            ),
                        shape = RoundedCornerShape(percent = 50)
                    ).drawBehind {
                        // Top highlight for metallic feel
                        if (enabled) {
                            val highlightHeight = 1.5.dp.toPx()
                            this.drawRoundRect(
                                color = Color.White.copy(alpha = 0.25f),
                                topLeft = Offset(8.dp.toPx(), 2.dp.toPx()),
                                size = this.size.copy(width = this.size.width - 16.dp.toPx(), height = highlightHeight),
                                cornerRadius = CornerRadius(highlightHeight / 2)
                            )
                        }
                    }.padding(horizontal = 8.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) contentColor else contentColor.copy(alpha = 0.4f)
                )
                Text(
                    text = label.uppercase(),
                    color = if (enabled) contentColor else contentColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    fontSize = 7.sp,
                    letterSpacing = 0.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
