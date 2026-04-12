package io.github.smithjustinn.blackjack.model

import androidx.compose.runtime.Immutable
import io.github.smithjustinn.blackjack.logic.GameRules
import kotlinx.serialization.Serializable

/**
 * User-persisted configuration and preferences for the Blackjack application.
 *
 * This data class represents the global settings that transcend individual game sessions,
 * governing audio behavior, default house rules, and automated interaction flow (like auto-deal).
 *
 * @property isSoundMuted If true, all auditory feedback (chips, cards, wins/losses) is suppressed.
 * @property gameRules The set of [GameRules] applied by default to each new [io.github.smithjustinn.blackjack.model.GameState].
 * @property defaultHandCount The initial number of hand seats (1-3) enabled when starting a new round.
 * @property isAutoDealEnabled If true, the system automatically dispatches [io.github.smithjustinn.blackjack.action.GameAction.Deal]
 *           after a round concludes (if valid bets exist), enabling a rapid, continuous play loop.
 */
@Immutable
@Serializable
data class AppSettings(
    val isSoundMuted: Boolean = false,
    val gameRules: GameRules = GameRules(),
    val defaultHandCount: Int = 1,
    val isAutoDealEnabled: Boolean = false,
)
