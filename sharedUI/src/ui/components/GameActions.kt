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
import androidx.compose.runtime.Composable
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
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.hit
import sharedui.generated.resources.stand

@Composable
fun GameActions(
    state: GameState,
    component: BlackjackComponent,
) {
    val audioService = LocalAppGraph.current.audioService
    AnimatedContent(
        targetState = state.status,
        transitionSpec = {
            fadeIn(animationSpec = tween(500)) togetherWith
                fadeOut(animationSpec = tween(500))
        },
        label = "GameActionsTransition"
    ) { status ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (status == GameStatus.PLAYING) {
                val canSplit = state.canSplit()
                val canDouble = state.canDoubleDown()

                // High-action row (Split/Double) - Fixed height to prevent shift
                Box(
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (canSplit || canDouble) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                        ) {
                            if (canDouble) {
                                ActionIcon(icon = "x2", label = "Double") {
                                    audioService.playEffect(AudioService.SoundEffect.DEAL)
                                    component.onAction(GameAction.DoubleDown)
                                }
                            }
                            if (canSplit) {
                                ActionIcon(icon = "⑃", label = "Split") {
                                    audioService.playEffect(AudioService.SoundEffect.DEAL)
                                    component.onAction(GameAction.Split)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CasinoButton(
                        text = stringResource(Res.string.hit),
                        onClick = {
                            audioService.playEffect(AudioService.SoundEffect.DEAL)
                            component.onAction(GameAction.Hit)
                        },
                        modifier = Modifier.weight(1f),
                        isStrategic = true,
                    )
                    CasinoButton(
                        text = stringResource(Res.string.stand),
                        onClick = {
                            audioService.playEffect(AudioService.SoundEffect.CLICK)
                            component.onAction(GameAction.Stand)
                        },
                        modifier = Modifier.weight(1f),
                        containerColor = GlassDark,
                        contentColor = Color.White,
                    )
                }
            } else if (status != GameStatus.INSURANCE_OFFERED) {
                // Reserve space matching the PLAYING state height (60 + 16 + 80 = 156.dp approx)
                Column(
                    modifier = Modifier.fillMaxWidth().height(156.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    CasinoButton(
                        text = "NEW GAME",
                        onClick = {
                            audioService.playEffect(AudioService.SoundEffect.FLIP)
                            component.onAction(GameAction.NewGame())
                        },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        isStrategic = true,
                    )
                }
            } else {
                // Insurance state - just a placeholder to maintain height if needed
                Spacer(modifier = Modifier.height(156.dp))
            }
        }
    }
}
