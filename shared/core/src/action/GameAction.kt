package io.github.smithjustinn.blackjack.action
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.SideBetType
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

/**
 * Represents a command or interface event dispatched to the [BlackjackStateMachine].
 *
 * Each [GameAction] variant corresponds to a specific player interaction or engine
 * request. The state machine processes these actions to transition the [GameState]
 * and emit [GameEffect]s.
 */
sealed class GameAction {
    /**
     * Resets the game to its initial state, potentially with a new balance and rule set.
     *
     * @param initialBalance The starting bankroll. If null, uses the current balance.
     * @param rules The [GameRules] to apply for the new session.
     * @param handCount The number of hands (1-3) to start the round with.
     * @param previousBets The list of main bets from the previous round for repeat-bet functionality.
     * @param lastSideBets The side bets from the previous round for repeat-bet functionality.
     */
    data class NewGame(
        val initialBalance: Int? = null,
        val rules: GameRules = GameRules(),
        val handCount: Int = 1,
        val previousBets: PersistentList<Int> = persistentListOf(0),
        val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    ) : GameAction()

    /**
     * Forfeits the current active hand, refunding half of the active bet back to the balance.
     *
     * Only valid at the start of a player's turn (exactly 2 cards) if [GameRules.allowSurrender] is true.
     */
    data object Surrender : GameAction()

    /**
     * Adds chips to the main bet for a specific seat index.
     *
     * Only valid during [GameStatus.BETTING].
     *
     * @param amount The number of chips to add to the bet.
     * @param seatIndex The 0-based seat index (0-2) being wagered on.
     */
    data class PlaceBet(
        val amount: Int,
        val seatIndex: Int = 0,
    ) : GameAction()

    /**
     * Resets all main bets on the table back to the player's balance. Only valid during [GameStatus.BETTING].
     * Note: This does not reset side bets. You must also dispatch [ResetSideBets] if you want a full reset.
     */
    data object ResetBet : GameAction()

    /**
     * Resets the main bet for a specific seat back to the player's balance.
     *
     * @param seatIndex The 0-based seat index (0-2) to clear.
     */
    data class ResetSeatBet(
        val seatIndex: Int
    ) : GameAction()

    /**
     * Finalizes the betting phase and initiates the initial deal of cards.
     *
     * Transitions [GameStatus.BETTING] -> [GameStatus.DEALING]. All active hands must have a bet > 0.
     */
    data object Deal : GameAction()

    /**
     * Requests an additional card for the [GameState.activeHand].
     *
     * If the hand busts (score > 21) after hitting, the turn automatically advances to the
     * next hand or the dealer.
     */
    data object Hit : GameAction()

    /**
     * Ends the turn for the [GameState.activeHand] without taking further cards.
     *
     * Advances to the next hand in [GameState.playerHands] or transitions to [GameStatus.DEALER_TURN].
     */
    data object Stand : GameAction()

    /**
     * Doubles the bet on the [GameState.activeHand] and takes exactly one more card.
     *
     * Only valid when the hand has exactly 2 cards and the player's balance can cover the
     * additional wager. Automatically ends the player's turn for that hand.
     */
    data object DoubleDown : GameAction()

    /**
     * Accepts the insurance offer when the dealer shows an Ace.
     *
     * Places a side bet equal to half of the [GameState.currentBet]. If the dealer has a
     * natural Blackjack, insurance pays 2:1 (effectively breaking even on the round).
     */
    data object TakeInsurance : GameAction()

    /** Declines the insurance offer and proceeds with the player's turn or dealer reveal. */
    data object DeclineInsurance : GameAction()

    /**
     * Splits a paired hand into two separate hands, each receiving a new card and a matching bet.
     *
     * Only valid when the [GameState.activeHand] has exactly 2 cards of equal rank (or value,
     * if [GameRules.splitOnValueOnly] is true) and the player has not exceeded [GameState.MAX_HANDS].
     */
    data object Split : GameAction()

    /**
     * Updates the number of initial seats (1-3) available for betting.
     *
     * @param count The number of hands to enable.
     */
    data class SelectHandCount(
        val count: Int
    ) : GameAction()

    /**
     * Updates the [GameRules] for the current session.
     *
     * Changes take effect during [GameStatus.BETTING].
     */
    data class UpdateRules(
        val rules: GameRules
    ) : GameAction()

    /**
     * Places a side bet (e.g., Perfect Pairs) on the current round.
     *
     * Side bets are settled immediately after the initial deal.
     *
     * @param type The [SideBetType] being wagered on.
     * @param amount The amount to wager.
     */
    data class PlaceSideBet(
        val type: SideBetType,
        val amount: Int
    ) : GameAction()

    /** Resets all active side bets on the table back to the player's balance. Only valid during [GameStatus.BETTING]. */
    data object ResetSideBets : GameAction()

    /**
     * Resets a specific side bet on the table back to the player's balance. Only valid during [GameStatus.BETTING].
     * @param type The specific [SideBetType] to clear.
     */
    data class ResetSideBet(
        val type: SideBetType
    ) : GameAction()

    // ── Internal engine primitives (dispatched by middleware only) ────────────────

    /** Replaces the current deck with a freshly computed shoe. Dispatched by middleware when reshuffle is needed. */
    internal data class SetDeck(
        val deck: PersistentList<Card>
    ) : GameAction()

    /** Draws the top deck card and adds it to a player seat's hand. */
    internal data class DealCardToPlayer(
        val seatIndex: Int
    ) : GameAction()

    /** Draws the top deck card and adds it to the dealer's hand. */
    internal data class DealCardToDealer(
        val faceDown: Boolean
    ) : GameAction()

    /** Resolves side bets, blackjacks, insurance offering, and early terminal states after the deal animation. */
    internal data object ApplyInitialOutcome : GameAction()

    /** Flips all face-down dealer cards face-up and pays out insurance if dealer has natural BJ. */
    internal data object RevealDealerHole : GameAction()

    /** Draws the top deck card and adds it to the dealer's hand during the dealer turn. */
    internal data object DealerDraw : GameAction()

    /** Calculates final hand results and transitions to a terminal status. */
    internal data object FinalizeGame : GameAction()
}
