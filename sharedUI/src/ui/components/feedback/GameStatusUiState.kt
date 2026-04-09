package io.github.smithjustinn.blackjack.ui.components.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.ui.theme.DeepWine
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.ModernGoldLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.ui.theme.TacticalRed
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.net_result_lost
import sharedui.generated.resources.net_result_push
import sharedui.generated.resources.net_result_won
import sharedui.generated.resources.status_announcement_template
import sharedui.generated.resources.status_blackjack_exclamation
import sharedui.generated.resources.status_bust
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_player_won

/**
 * Pre-mapped UI state for [GameStatusMessage] — keeps domain decisions (status text, outcome
 * colours, net result labels) out of the rendering Composable.
 */
data class GameStatusUiState(
    val statusText: String,
    val netLabel: String?,
    val screenReaderAnnouncement: String,
    val accentColor: Color,
    val bannerBackgroundTopColor: Color,
    val netLabelColor: Color,
    val isTerminal: Boolean,
    /** True when the player won; drives ring-expansion and shimmer animations. */
    val isPlayerWon: Boolean,
    /** True for blackjack win; drives faster pulse and shimmer timing. */
    val isBlackjack: Boolean,
    /** True when the shimmer sweep should play (PLAYER_WON or PUSH). */
    val showShimmer: Boolean,
    /** Raw payout value retained for the count-up animation in the rendering layer. */
    val netPayout: Int?,
)

// UI state mapper: keeps domain decisions (status text, outcome colours) out of Composable scope.
@Composable
fun rememberGameStatusUiState(
    status: GameStatus,
    isBlackjack: Boolean,
    isBust: Boolean,
    netPayout: Int?,
): GameStatusUiState {
    val statusText =
        when {
            isBlackjack -> stringResource(Res.string.status_blackjack_exclamation)
            isBust -> stringResource(Res.string.status_bust)
            status == GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            status == GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            else -> ""
        }
    val isTerminal = status.isTerminal()
    val netLabel: String? =
        if (isTerminal && netPayout != null) {
            when {
                netPayout > 0 -> stringResource(Res.string.net_result_won, netPayout)
                netPayout < 0 -> stringResource(Res.string.net_result_lost, -netPayout)
                else -> stringResource(Res.string.net_result_push)
            }
        } else {
            null
        }
    val screenReaderAnnouncement =
        if (netLabel != null) {
            stringResource(Res.string.status_announcement_template, statusText, netLabel)
        } else {
            statusText
        }
    val accentColor =
        when (status) {
            GameStatus.PLAYER_WON -> ModernGoldLight
            GameStatus.DEALER_WON -> TacticalRed
            GameStatus.PUSH -> Color.White
            else -> ModernGoldLight.copy(alpha = 0.8f)
        }
    val bannerBackgroundTopColor =
        when {
            isBlackjack -> PrimaryGold.copy(alpha = 0.15f)
            status == GameStatus.PLAYER_WON -> FeltGreen.copy(alpha = 0.85f)
            status == GameStatus.DEALER_WON -> DeepWine.copy(alpha = 0.85f)
            status == GameStatus.PUSH -> Color.Gray.copy(alpha = 0.85f)
            else -> DeepWine.copy(alpha = 0.85f)
        }
    val netLabelColor =
        when {
            netPayout != null && netPayout > 0 -> PrimaryGold
            netPayout != null && netPayout < 0 -> TacticalRed
            else -> Color.White.copy(alpha = 0.7f)
        }
    return GameStatusUiState(
        statusText = statusText,
        netLabel = netLabel,
        screenReaderAnnouncement = screenReaderAnnouncement,
        accentColor = accentColor,
        bannerBackgroundTopColor = bannerBackgroundTopColor,
        netLabelColor = netLabelColor,
        isTerminal = isTerminal,
        isPlayerWon = status == GameStatus.PLAYER_WON,
        isBlackjack = isBlackjack,
        showShimmer = status == GameStatus.PLAYER_WON || status == GameStatus.PUSH,
        netPayout = netPayout,
    )
}
