package io.github.smithjustinn.blackjack.ui.components.actions

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.action_double
import sharedui.generated.resources.action_hit
import sharedui.generated.resources.action_new_game
import sharedui.generated.resources.action_split
import sharedui.generated.resources.action_stand

/**
 * A premium, highly-animated button component designed for the casino environment.
 *
 * This button features a 3D "embossed" look with vertical gradients, automated scale-spring
 * animations on press, and specialized visual treatments for strategic actions and "juicy" highlights.
 *
 * @param text The label displayed on the button. Automatically transformed to uppercase.
 * @param onClick Callback invoked when the button is tapped. Only active if [enabled] is true.
 * @param modifier [Modifier] applied to the root of the button layout.
 * @param enabled When false, the button is non-interactive and appears desaturated with a "glass" texture.
 * @param isStrategic When true, the button uses [PrimaryGold] to signal it as the recommended
 *        strategic move (e.g., as suggested by the house or basic strategy logic).
 * @param showShine When true, triggers a repeating diagonal "shine" sweep animation across the button
 *        surface to draw user attention.
 * @param containerColor Optional override for the button's background color. Defaults based on [isStrategic].
 * @param contentColor Optional override for the text label color. Defaults based on [isStrategic].
 * @param contentPadding Internal [PaddingValues] around the text label.
 * @param contentDescription Text read by accessibility services to describe the button's action.
 * @param stateDescription Text read by accessibility services to describe the button's current state.
 */
@Composable
fun CasinoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isStrategic: Boolean = false,
    showShine: Boolean = false,
    containerColor: Color? = null,
    contentColor: Color? = null,
    contentPadding: PaddingValues =
        PaddingValues(horizontal = 24.dp, vertical = 20.dp),
    contentDescription: String? = null,
    stateDescription: String? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scaleState =
        animateFloatAsState(
            targetValue = if (isPressed) 0.95f else 1f,
            animationSpec =
                if (isPressed) {
                    tween(durationMillis = 80)
                } else {
                    spring(dampingRatio = 0.4f, stiffness = 400f)
                },
            label = "buttonScale",
        )

    val offsetYState =
        animateFloatAsState(
            targetValue = if (isPressed) 4f else 0f,
            animationSpec =
                if (isPressed) {
                    tween(durationMillis = 80)
                } else {
                    spring(dampingRatio = 0.5f, stiffness = 400f)
                },
            label = "buttonOffset",
        )

    // Conditional shine loop — only consumes GPU when both showShine and enabled are true.
    // Keying on both cancels the coroutine immediately when either turns false.
    val shineX = remember { Animatable(-1f) }
    LaunchedEffect(showShine, enabled) {
        if (showShine && enabled) {
            while (true) {
                shineX.snapTo(-1f)
                shineX.animateTo(
                    targetValue = 2f,
                    animationSpec =
                        tween(
                            durationMillis = AnimationConstants.ButtonShineDuration,
                            easing = LinearEasing,
                        ),
                )
                kotlinx.coroutines.delay(AnimationConstants.ButtonShineDelay.toLong())
            }
        } else {
            shineX.snapTo(-1f)
        }
    }

    val resolvedContainerColor = containerColor ?: if (isStrategic) PrimaryGold else MaterialTheme.colorScheme.secondary
    val resolvedContentColor =
        contentColor ?: if (isStrategic) BackgroundDark else MaterialTheme.colorScheme.onSecondary

    // Disabled colors: desaturated and darker
    val baseColor = resolvedContainerColor

    val shadowColor = if (enabled) Color.Black.copy(alpha = 0.6f) else Color.Transparent

    Box(
        modifier =
            modifier
                .graphicsLayer {
                    // Bolt Performance Optimization: Defer animation state reads (scale, offsetY)
                    // to the layout/draw phase inside graphicsLayer. This eliminates O(frames) recompositions
                    // of the entire button during press-back or continuous animations.
                    scaleX = scaleState.value
                    scaleY = scaleState.value
                    translationY = offsetYState.value
                }.then(
                    if (enabled) {
                        Modifier.shadow(
                            elevation = if (isPressed) 2.dp else 8.dp,
                            shape = RoundedCornerShape(16.dp),
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    } else {
                        Modifier
                    }
                ).clip(RoundedCornerShape(16.dp))
                .background(
                    if (enabled) {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    // Top highlight reflection
                                    Color(
                                        (baseColor.red * 1.15f).coerceIn(0f, 1f),
                                        (baseColor.green * 1.15f).coerceIn(0f, 1f),
                                        (baseColor.blue * 1.15f).coerceIn(0f, 1f),
                                        baseColor.alpha
                                    ),
                                    baseColor,
                                    // Deep shadow bottom
                                    Color(
                                        (baseColor.red * 0.7f).coerceIn(0f, 1f),
                                        (baseColor.green * 0.7f).coerceIn(0f, 1f),
                                        (baseColor.blue * 0.7f).coerceIn(0f, 1f),
                                        baseColor.alpha
                                    )
                                )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors =
                                listOf(
                                    GlassDark,
                                    GlassDark
                                )
                        )
                    }
                ).then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            color = Color.White,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else if (enabled) {
                        // Premium acrylic double border: soft bright inner top, dark edge bottom
                        Modifier.border(
                            width = 1.dp,
                            brush =
                                Brush.verticalGradient(
                                    colors =
                                        listOf(
                                            Color.White.copy(alpha = 0.5f),
                                            Color.White.copy(alpha = 0.1f),
                                            Color.Black.copy(alpha = 0.4f)
                                        )
                                ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            color =
                                if (isStrategic) {
                                    PrimaryGold.copy(
                                        alpha = 0.2f
                                    )
                                } else {
                                    Color.White.copy(alpha = 0.1f)
                                },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                ).drawBehind {
                    // Read shineX.value inside draw scope — deferred from composition,
                    // so only this draw lambda re-runs each animation frame.
                    val currentShineX = shineX.value
                    if (currentShineX > -1f) {
                        val pixelX = currentShineX * size.width
                        val bandWidth = size.width * 0.35f
                        drawRect(
                            brush =
                                Brush.linearGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.45f),
                                            Color.Transparent
                                        ),
                                    start = Offset(pixelX, 0f),
                                    end = Offset(pixelX + bandWidth, size.height)
                                )
                        )
                    }
                }.semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription?.let { this.contentDescription = it }
                    stateDescription?.let { this.stateDescription = it }
                }.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ).padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        val disabledContentColor =
            if (isStrategic) {
                PrimaryGold
                    .copy(alpha = 0.3f)
            } else {
                resolvedContentColor.copy(alpha = 0.3f)
            }

        Text(
            text = text.uppercase(),
            color = if (enabled) resolvedContentColor else disabledContentColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 2.sp
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun CasinoButtonPreview() {
    BlackjackTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            CasinoButton(text = stringResource(Res.string.action_hit), onClick = { /* do nothing */ })
            CasinoButton(
                text = stringResource(Res.string.action_stand),
                onClick = { /* do nothing */ },
                modifier = Modifier.padding(top = 8.dp)
            )
            CasinoButton(
                text = stringResource(Res.string.action_double),
                onClick = { /* do nothing */ },
                isStrategic = true,
                modifier = Modifier.padding(top = 8.dp)
            )
            CasinoButton(
                text = stringResource(Res.string.action_split),
                onClick = { /* do nothing */ },
                enabled = false,
                modifier = Modifier.padding(top = 8.dp)
            )
            CasinoButton(
                text = stringResource(Res.string.action_new_game),
                onClick = { /* do nothing */ },
                showShine = true,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
