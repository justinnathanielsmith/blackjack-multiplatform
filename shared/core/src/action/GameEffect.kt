package io.github.smithjustinn.blackjack.action
import io.github.smithjustinn.blackjack.model.SideBetType

/**
 * Represents a transient visual or auditory side effect triggered by game events.
 *
 * Effects are decoupled from the [GameState] and are emitted via the [BlackjackStateMachine.effects]
 * flow to be consumed by the UI layer for "juicy" feedback (sounds, animations, haptics).
 */
sealed class GameEffect {
    /** Plays the standard [AudioService.SoundEffect.FLIP] sound when a card is dealt or revealed. */
    data object PlayCardSound : GameEffect()

    /** Plays [AudioService.SoundEffect.WIN] and triggers high-payout visual feedback. */
    data object PlayWinSound : GameEffect()

    /** Plays [AudioService.SoundEffect.LOSE] and triggers loss-related visual feedback. */
    data object PlayLoseSound : GameEffect()

    /** Triggers a standard device vibration. Used for errors or important status changes. */
    data object Vibrate : GameEffect()

    /** Plays [AudioService.SoundEffect.TENSION] when the dealer is about to draw on a "stiff" hand. */
    data object DealerCriticalDraw : GameEffect()

    /**
     * Highlights a specific hand in the UI.
     *
     * Triggered when a player hits and achieves a "power" score (exactly 11), signaling
     * a strong position for doubling or further hits.
     *
     * @param handIndex The 0-based index of the hand to highlight.
     */
    data class NearMissHighlight(
        val handIndex: Int
    ) : GameEffect()

    /** Triggers a heavy haptic thud, often used when a face card (value 10) is dealt. */
    data object HeavyCardThud : GameEffect()

    /** Triggers a haptic pulse when a hand reaches exactly 21. */
    data object Pulse21 : GameEffect()

    /** Triggers a subtle haptic tick for interactions or low-value card deals. */
    data object LightTick : GameEffect()

    /** Triggers a strong haptic burst when a player wins a round. */
    data object WinPulse : GameEffect()

    /** Triggers a distinct haptic thud when a hand busts (exceeds 21). */
    data object BustThud : GameEffect()

    /**
     * Triggers a "chip eruption" animation where chips fly from the table to the player's balance.
     *
     * @param amount The total number of chips in the eruption.
     * @param sideBetType Optional type of the side bet that triggered this payout.
     */
    data class ChipEruption(
        val amount: Int,
        val sideBetType: SideBetType? = null
    ) : GameEffect()

    /**
     * Triggers a "chip loss" animation where chips fly from the table towards the dealer/shoe.
     *
     * @param amount The total number of chips lost.
     */
    data class ChipLoss(
        val amount: Int
    ) : GameEffect()

    /** Plays a subtle "plink" sound, typically used for chip interactions or selection. */
    data object PlayPlinkSound : GameEffect()

    /** Plays [AudioService.SoundEffect.PUSH] when a hand ends in a tie. */
    data object PlayPushSound : GameEffect()

    /**
     * Signals a massive side-bet win (payoutMultiplier >= 25).
     * Suppresses [PlayWinSound]; the orchestrator plays THE_NUTS instead and shows the BigWinBanner.
     * @param totalPayout Combined side-bet payout amount to display in the banner.
     */
    data class BigWin(
        val totalPayout: Int
    ) : GameEffect()
}
