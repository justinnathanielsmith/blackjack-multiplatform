package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.BlackjackPayout
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.close
import sharedui.generated.resources.settings_das
import sharedui.generated.resources.settings_decks
import sharedui.generated.resources.settings_game_rules
import sharedui.generated.resources.settings_mute
import sharedui.generated.resources.settings_payout
import sharedui.generated.resources.settings_reset_balance
import sharedui.generated.resources.settings_rule_disclaimer
import sharedui.generated.resources.settings_s17
import sharedui.generated.resources.settings_surrender
import sharedui.generated.resources.settings_title

@Composable
fun SettingsOverlay(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onResetBalance: () -> Unit,
    onDismiss: () -> Unit
) {
    var showResetConfirmation by remember { mutableStateOf(false) }

    BaseOverlay(
        title = stringResource(Res.string.settings_title),
        onDismiss = onDismiss
    ) {
        // App Settings
        SettingsToggle(
            title = stringResource(Res.string.settings_mute),
            checked = settings.isSoundMuted,
            onCheckedChange = { newVal -> onUpdateSettings { it.copy(isSoundMuted = newVal) } }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { showResetConfirmation = true },
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8B0000),
                    contentColor = Color.White
                ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                stringResource(Res.string.settings_reset_balance).uppercase(),
                style = MaterialTheme.typography.labelLarge
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.White.copy(alpha = 0.1f))

        // Game Rules
        Text(
            text = stringResource(Res.string.settings_game_rules).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsDropdown(
            title = stringResource(Res.string.settings_payout),
            currentValue =
                when (settings.gameRules.blackjackPayout) {
                    BlackjackPayout.THREE_TO_TWO -> "3:2"
                    BlackjackPayout.SIX_TO_FIVE -> "6:5"
                },
            options = listOf("3:2", "6:5"),
            onOptionSelected = { option ->
                onUpdateSettings {
                    it.copy(
                        gameRules =
                            it.gameRules.copy(
                                blackjackPayout =
                                    if (option ==
                                        "3:2"
                                    ) {
                                        BlackjackPayout.THREE_TO_TWO
                                    } else {
                                        BlackjackPayout.SIX_TO_FIVE
                                    }
                            )
                    )
                }
            }
        )

        SettingsToggle(
            title = stringResource(Res.string.settings_s17),
            checked = settings.gameRules.dealerHitsSoft17,
            onCheckedChange = { newVal ->
                onUpdateSettings { it.copy(gameRules = it.gameRules.copy(dealerHitsSoft17 = newVal)) }
            }
        )

        SettingsToggle(
            title = stringResource(Res.string.settings_das),
            checked = settings.gameRules.allowDoubleAfterSplit,
            onCheckedChange = { newVal ->
                onUpdateSettings { it.copy(gameRules = it.gameRules.copy(allowDoubleAfterSplit = newVal)) }
            }
        )

        SettingsToggle(
            title = stringResource(Res.string.settings_surrender),
            checked = settings.gameRules.allowSurrender,
            onCheckedChange = { newVal ->
                onUpdateSettings { it.copy(gameRules = it.gameRules.copy(allowSurrender = newVal)) }
            }
        )

        SettingsDropdown(
            title = stringResource(Res.string.settings_decks),
            currentValue = "${settings.gameRules.deckCount}",
            options = listOf("1", "2", "4", "6", "8"),
            onOptionSelected = { option ->
                onUpdateSettings {
                    it.copy(gameRules = it.gameRules.copy(deckCount = option.toInt()))
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.settings_rule_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.4f)
        )

        if (showResetConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetConfirmation = false },
                title = {
                    Text(
                        text = stringResource(Res.string.settings_reset_balance).uppercase(),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to reset your balance? This action cannot be undone."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showResetConfirmation = false
                            onResetBalance()
                        },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF8B0000),
                                contentColor = Color.White
                            )
                    ) {
                        Text(stringResource(Res.string.settings_reset_balance).uppercase())
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showResetConfirmation = false },
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = PrimaryGold,
                                contentColor = Color.Black
                            )
                    ) {
                        Text(stringResource(Res.string.close).uppercase())
                    }
                },
                containerColor = GlassDark,
                titleContentColor = PrimaryGold,
                textContentColor = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange
                ).padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = PrimaryGold,
                    checkedTrackColor = PrimaryGold.copy(alpha = 0.5f)
                )
        )
    }
}

@Composable
private fun SettingsDropdown(
    title: String,
    currentValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable(role = Role.DropdownList) { expanded = true }
                .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White)
        Box {
            Surface(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp)),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Text(
                    text = currentValue,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(GlassDark)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = Color.White) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
