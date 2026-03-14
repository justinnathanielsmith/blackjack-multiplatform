package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.screens.LayoutMode
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.double_down
import sharedui.generated.resources.hit
import sharedui.generated.resources.new_game
import sharedui.generated.resources.split
import sharedui.generated.resources.stand

@Composable
fun GameActions(
    state: GameState,
    component: BlackjackComponent,
    layoutMode: LayoutMode = LayoutMode.PORTRAIT,
) {
    val audioService = LocalAppGraph.current.audioService
    val appSettings by component.appSettings.collectAsState()

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
    val onSurrender =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.Surrender)
            }
        }
    val onNewGame =
        remember(audioService, component, appSettings) {
            {
                audioService.playEffect(AudioService.SoundEffect.FLIP)
                component.onAction(GameAction.NewGame(rules = appSettings.gameRules, handCount = appSettings.defaultHandCount))
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
        val isCompact = layoutMode == LayoutMode.LANDSCAPE_COMPACT
        val buttonHeight = if (isCompact) 48.dp else 80.dp
        val spacerHeight = if (isCompact) 6.dp else 16.dp
        val totalActionsHeight = (buttonHeight * 2) + spacerHeight // Reserved space for two rows of buttons

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacerHeight),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (status == GameStatus.PLAYING) {
                val canSplit = state.canSplit()
                val canDouble = state.canDoubleDown()

                // High-action row (Split/Double) - Fixed height to prevent shift
                Box(
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (canSplit || canDouble) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (canDouble) {
                                GameActionButton(
                                    icon = "x2",
                                    label = stringResource(Res.string.double_down),
                                    onClick = onDoubleDown,
                                    modifier = Modifier.weight(1f).height(buttonHeight),
                                    isStrategic = true,
                                )
                            } else if (canSplit) {
                                // Provide empty space to keep grid consistent if only split is available
                                Spacer(modifier = Modifier.weight(1f))
                            }
                            
                            if (canSplit) {
                                GameActionButton(
                                    icon = "⑃",
                                    label = stringResource(Res.string.split),
                                    onClick = onSplit,
                                    modifier = Modifier.weight(1f).height(buttonHeight),
                                )
                            } else if (canDouble) {
                                // Provide empty space to keep grid consistent if only double is available
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    GameActionButton(
                        icon = "👇",
                        label = stringResource(Res.string.hit),
                        onClick = onHit,
                        modifier = Modifier.weight(1f).height(buttonHeight),
                        isStrategic = true,
                    )
                    GameActionButton(
                        icon = "✋",
                        label = stringResource(Res.string.stand),
                        onClick = onStand,
                        modifier = Modifier.weight(1f).height(buttonHeight),
                    )
                }
            } else if (status != GameStatus.INSURANCE_OFFERED) {
                // Reserve space matching the PLAYING state height
                Column(
                    modifier = Modifier.fillMaxWidth().height(totalActionsHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CasinoButton(
                        text = stringResource(Res.string.new_game),
                        onClick = onNewGame,
                        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth().height(buttonHeight),
                        isStrategic = true,
                    )
                }
            } else {
                // Insurance state - just a placeholder to maintain height if needed
                Spacer(modifier = Modifier.height(totalActionsHeight))
            }
        }
    }
}
