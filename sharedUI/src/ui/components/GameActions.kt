package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.ic_double
import sharedui.generated.resources.ic_hit
import sharedui.generated.resources.ic_split
import sharedui.generated.resources.ic_stand
import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import sharedui.generated.resources.action_hit
import sharedui.generated.resources.action_stand
import sharedui.generated.resources.action_double
import sharedui.generated.resources.action_split

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
    val onSurrender =
        remember(audioService, component) {
            {
                audioService.playEffect(AudioService.SoundEffect.CLICK)
                component.onAction(GameAction.Surrender)
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
        val totalActionsHeight = buttonHeight

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AnimatedVisibility(
                visible = status == GameStatus.PLAYING,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                exit = fadeOut(tween(300)) + shrinkVertically(tween(300)),
            ) {
                val canSplit = state.canSplit()
                val canDouble = state.canDoubleDown()

                Row(
                    modifier = Modifier.fillMaxWidth().height(buttonHeight),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    GameActionButton(
                        icon = Res.drawable.ic_double,
                        onClick = onDoubleDown,
                        enabled = canDouble,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        isStrategic = true,
                        label = stringResource(Res.string.action_double),
                    )
                    GameActionButton(
                        icon = Res.drawable.ic_split,
                        onClick = onSplit,
                        enabled = canSplit,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        label = stringResource(Res.string.action_split),
                    )
                    GameActionButton(
                        icon = Res.drawable.ic_hit,
                        onClick = onHit,
                        enabled = true,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        containerColor = ChipGreen,
                        contentColor = Color.White,
                        label = stringResource(Res.string.action_hit),
                    )
                    GameActionButton(
                        icon = Res.drawable.ic_stand,
                        onClick = onStand,
                        enabled = true,
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        containerColor = TacticalRed,
                        contentColor = Color.White,
                        label = stringResource(Res.string.action_stand),
                    )
                }
            }

            if (status != GameStatus.PLAYING && status != GameStatus.INSURANCE_OFFERED) {
                Spacer(modifier = Modifier.height(totalActionsHeight))
            } else if (status == GameStatus.INSURANCE_OFFERED) {
                Spacer(modifier = Modifier.height(totalActionsHeight))
            }
        }
    }
}
