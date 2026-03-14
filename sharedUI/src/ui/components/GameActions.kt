package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.deal
import sharedui.generated.resources.hit
import sharedui.generated.resources.stand

@Composable
fun GameActions(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.status == GameStatus.PLAYING) {
            val canSplit = state.canSplit()
            val canDouble = state.canDoubleDown()
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
        } else if (state.status != GameStatus.INSURANCE_OFFERED) {
            CasinoButton(
                text = "NEW GAME",
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.NewGame())
                },
                modifier = Modifier.fillMaxWidth(),
                isStrategic = true,
            )
        }
    }
}
