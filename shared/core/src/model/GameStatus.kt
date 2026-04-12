package io.github.smithjustinn.blackjack.model

import kotlinx.serialization.Serializable

/**
 * The lifecycle state of a single Blackjack round.
 *
 * This enum governs the **Phase Gating** of the entire engine. It determines which
 * [io.github.smithjustinn.blackjack.action.GameAction]s are valid and which UI
 * overlays are displayed.
 *
 * **Lifecycle Flow:**
 * [BETTING] → [DEALING] → ([INSURANCE_OFFERED]) → [PLAYING] → [DEALER_TURN] → terminal outcome.
 *
 * **Functional Intent:**
 * - **State Mutability**: Actions only mutate the state when the status allows (e.g., no doubling during [DEALER_TURN]).
 * - **Input Locking**: Statuses where [isProcess] is true (DEALING, DEALER_TURN) must lock UI interaction.
 * - **Uninitialized Anchor**: [IDLE] acts as the sentinel between active game sessions.
 */
@Serializable
enum class GameStatus {
    /** The player is currently placing bets and configuring the game. No cards have been dealt. */
    BETTING,

    /** Transient state while the initial cards are being dealt with animations. Prevents player interaction. */
    DEALING,

    /** Default uninitialized state, used as a placeholder before a game session starts. */
    IDLE,

    /** The round is active and the player is making decisions (Hit, Stand, etc.) for their hands. */
    PLAYING,

    /**
     * Specialized state occurring if the dealer's upcard is an Ace.
     * The player is offered an insurance side-bet before cards are further revealed.
     */
    INSURANCE_OFFERED,

    /** The dealer is revealing their hole card and drawing cards according to house rules. */
    DEALER_TURN,

    /** Terminal state: The player has won the round. Winnings are added to the balance. */
    PLAYER_WON,

    /** Terminal state: The dealer (house) has won the round. Bets are forfeited. */
    DEALER_WON,

    /** Terminal state: The round ended in a tie. Original bets are returned to the player. */
    PUSH
}

/** Returns true if the status represents a final, settled outcome of a round. */
fun GameStatus.isTerminal() = this == GameStatus.PLAYER_WON || this == GameStatus.DEALER_WON || this == GameStatus.PUSH

/** Returns true if the status represents an active animation or engine process where input is locked. */
fun GameStatus.isProcess() = this == GameStatus.DEALING || this == GameStatus.DEALER_TURN

/** Returns true if the UI should display the game result overlay or progress indicators based on this status. */
fun GameStatus.isStatusVisible() = this.isProcess() || this.isTerminal()
