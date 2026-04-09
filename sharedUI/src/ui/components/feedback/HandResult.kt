package io.github.smithjustinn.blackjack.ui.components.feedback

import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.HandOutcome
import io.github.smithjustinn.blackjack.isTerminal

enum class HandResult {
    NONE,
    WIN,
    LOSS,
    PUSH
}

// Presentation mapper: isolates domain→UI enum translation from both the component and screen layers.
internal fun GameState.handResult(index: Int): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val domainOutcome = handOutcomes.getOrNull(index) ?: return HandResult.NONE
    return when (domainOutcome) {
        HandOutcome.WIN, HandOutcome.NATURAL_WIN -> HandResult.WIN
        HandOutcome.PUSH -> HandResult.PUSH
        HandOutcome.LOSS -> HandResult.LOSS
    }
}
