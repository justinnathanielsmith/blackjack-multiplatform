package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.bullet_point_template
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

/**
 * A modal overlay displaying the comprehensive rules, available actions, and side bet details for the Blackjack game.
 *
 * This component organizes the game's manual into distinct thematic sections (e.g., Core Rules, Actions,
 * Variations, and Side Bets) to provide players with a quick, in-game reference guide. It utilizes [BaseOverlay]
 * for consistent modal presentation and dismissal behavior across the application.
 *
 * @param onDismiss Callback invoked when the user requests to close the overlay (e.g., by tapping a close button or dismissing the modal).
 * @param modifier [Modifier] applied to the root layout of this overlay.
 */
@Composable
fun RulesOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BaseOverlay(
        title = stringResource(Res.string.rules_title),
        onDismiss = onDismiss,
        modifier = modifier
    ) {
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
                    stringResource(Res.string.rules_actions_split),
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
                    stringResource(Res.string.rules_side_bets_21_3),
                )
        )
    }
}

@Composable
private fun RuleSection(
    title: String,
    modifier: Modifier = Modifier,
    content: String? = null,
    items: List<String>? = null,
) {
    Column(modifier = modifier.padding(bottom = 20.dp)) {
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
                text = stringResource(Res.string.bullet_point_template, item),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}
