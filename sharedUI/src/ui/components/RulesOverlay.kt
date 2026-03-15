package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.close
import sharedui.generated.resources.rules_actions_double
import sharedui.generated.resources.rules_actions_hit
import sharedui.generated.resources.rules_actions_split
import sharedui.generated.resources.rules_actions_stand
import sharedui.generated.resources.rules_actions_title
import sharedui.generated.resources.rules_core_content
import sharedui.generated.resources.rules_core_title
import sharedui.generated.resources.rules_side_bets_21_3
import sharedui.generated.resources.rules_side_bets_pairs
import sharedui.generated.resources.rules_side_bets_title
import sharedui.generated.resources.rules_title
import sharedui.generated.resources.rules_variations_content
import sharedui.generated.resources.rules_variations_title

@Composable
fun RulesOverlay(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = GlassDark,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier =
                    Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(Res.string.rules_title).uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                RuleSection(
                    title = stringResource(Res.string.rules_core_title),
                    content = stringResource(Res.string.rules_core_content)
                )

                RuleSection(
                    title = stringResource(Res.string.rules_actions_title),
                    items =
                        listOf(
                            stringResource(Res.string.rules_actions_hit),
                            stringResource(Res.string.rules_actions_stand),
                            stringResource(Res.string.rules_actions_double),
                            stringResource(Res.string.rules_actions_split)
                        )
                )

                RuleSection(
                    title = stringResource(Res.string.rules_variations_title),
                    content = stringResource(Res.string.rules_variations_content)
                )

                RuleSection(
                    title = stringResource(Res.string.rules_side_bets_title),
                    items =
                        listOf(
                            stringResource(Res.string.rules_side_bets_pairs),
                            stringResource(Res.string.rules_side_bets_21_3)
                        )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = PrimaryGold,
                            contentColor = Color.Black
                        )
                ) {
                    Text(stringResource(Res.string.close).uppercase())
                }
            }
        }
    }
}

@Composable
private fun RuleSection(
    title: String,
    content: String? = null,
    items: List<String>? = null
) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White.copy(alpha = 0.5f),
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (content != null) {
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
        items?.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
