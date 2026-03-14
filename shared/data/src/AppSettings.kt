package io.github.smithjustinn.blackjack.data

import io.github.smithjustinn.blackjack.GameRules
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val isSoundMuted: Boolean = false,
    val isDebugMode: Boolean = false,
    val gameRules: GameRules = GameRules(),
)
