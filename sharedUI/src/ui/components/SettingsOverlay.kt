package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.smithjustinn.blackjack.BlackjackPayout
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.*

@Composable
fun SettingsOverlay(
    settings: AppSettings,
    onUpdateSettings: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = GlassDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.settings_title).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App Settings
                SettingsToggle(
                    title = stringResource(Res.string.settings_mute),
                    checked = settings.isSoundMuted,
                    onCheckedChange = { newVal -> onUpdateSettings { it.copy(isSoundMuted = newVal) } }
                )

                SettingsToggle(
                    title = stringResource(Res.string.settings_debug),
                    checked = settings.isDebugMode,
                    onCheckedChange = { newVal -> onUpdateSettings { it.copy(isDebugMode = newVal) } }
                )

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
                    currentValue = when (settings.gameRules.blackjackPayout) {
                        BlackjackPayout.THREE_TO_TWO -> "3:2"
                        BlackjackPayout.SIX_TO_FIVE -> "6:5"
                    },
                    options = listOf("3:2", "6:5"),
                    onOptionSelected = { option ->
                        onUpdateSettings {
                            it.copy(gameRules = it.gameRules.copy(
                                blackjackPayout = if (option == "3:2") BlackjackPayout.THREE_TO_TWO else BlackjackPayout.SIX_TO_FIVE
                            ))
                        }
                    }
                )

                SettingsToggle(
                    title = stringResource(Res.string.settings_s17),
                    checked = settings.gameRules.dealerHitsSoft17,
                    onCheckedChange = { newVal -> onUpdateSettings { it.copy(gameRules = it.gameRules.copy(dealerHitsSoft17 = newVal)) } }
                )

                SettingsToggle(
                    title = stringResource(Res.string.settings_das),
                    checked = settings.gameRules.allowDoubleAfterSplit,
                    onCheckedChange = { newVal -> onUpdateSettings { it.copy(gameRules = it.gameRules.copy(allowDoubleAfterSplit = newVal)) } }
                )

                SettingsToggle(
                    title = stringResource(Res.string.settings_surrender),
                    checked = settings.gameRules.allowSurrender,
                    onCheckedChange = { newVal -> onUpdateSettings { it.copy(gameRules = it.gameRules.copy(allowSurrender = newVal)) } }
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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGold, contentColor = Color.Black)
                ) {
                    Text(stringResource(Res.string.close).uppercase())
                }
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White)
        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { expanded = true },
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
