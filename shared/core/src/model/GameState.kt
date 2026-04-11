package io.github.smithjustinn.blackjack.model
import androidx.compose.runtime.Immutable
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.GameRules
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

/**
 * Represents the complete, immutable state of a Blackjack game at a single point in time.
 *
 * This state is the single source of truth for the [BlackjackStateMachine]. It encompasses the
 * deck, all hands (player and dealer), the current game status, and the player's economic state
 * (balance and bets).
 *
 * Multi-hand support (up to [MAX_HANDS]) is built-in via the [playerHands] list and the
 * [activeHandIndex].
 *
 * @property deck The current shoe of cards, represented as a persistent list.
 * @property playerHands The list of [Hand]s currently held by the player. Can grow during
 *           splits (up to [MAX_HANDS]).
 * @property activeHandIndex The 0-based index of the hand currently being played by the player
 *           in the [playerHands] list.
 * @property handCount The initial number of hands (1-3) the player started the round with.
 *           Note that [playerHands] size may differ from this after splitting.
 * @property dealerHand The dealer's [Hand].
 * @property status The current lifecycle phase of the game (e.g., [GameStatus.BETTING],
 *           [GameStatus.PLAYING]).
 * @property balance The player's current total bankroll, excluding active bets.
 * @property insuranceBet The amount wagered on insurance (only non-zero when the dealer shows
 *           an Ace and the player accepts the offer).
 * @property sideBets The set of active side bets (fixed until settled) currently on the table.
 * @property sideBetResults Settled results for side bets, calculated immediately after the deal.
 * @property lastSideBets Captured side bets from the previous round, enabling "repeat bet".
 * @property lastBets Captured main bets from the previous round across hands, enabling "repeat bet".
 * @property rules The [GameRules] currently in effect for this session.
 * @property dealerDrawIsCritical True if the dealer is currently in a state that triggers
 *           visual "critical" indications (e.g., potentially busting or drawing on the edge).
 */
@Immutable
@Serializable
data class GameState(
    val deck: PersistentList<Card> = persistentListOf(),
    val playerHands: PersistentList<Hand> = persistentListOf(Hand()),
    val activeHandIndex: Int = 0,
    val handCount: Int = 1,
    val dealerHand: Hand = Hand(),
    val status: GameStatus = GameStatus.IDLE,
    val balance: Int = BlackjackConfig.INITIAL_BALANCE,
    val insuranceBet: Int = 0,
    val sideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val sideBetResults: PersistentMap<SideBetType, SideBetResult> = persistentMapOf(),
    val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val lastBets: PersistentList<Int> = persistentListOf(0),
    val rules: GameRules = GameRules(),
    val dealerDrawIsCritical: Boolean = false,
    val handOutcomes: PersistentList<HandOutcome> = persistentListOf(),
) {
    /**
     * Alias for the bet on the first hand. Used for backward compatibility with single-hand
     * systems (like insurance and certain result calculations).
     */
    val currentBet: Int get() = playerHands.getOrNull(0)?.bet ?: 0

    /**
     * The total amount currently wagered on the table across all player hands,
     * active side bets, and insurance. Once side bets are settled ([sideBetResults] is not empty),
     * they are no longer counted towards the total table bet.
     */
    val totalBet: Int =
        run {
            var mainBetsTotal = 0
            for (i in 0 until playerHands.size) {
                mainBetsTotal += playerHands[i].bet
            }

            // Only count active side bets (before they are settled)
            val sideBetsTotal =
                if (sideBetResults.isEmpty()) {
                    var sum = 0
                    for (bet in sideBets.values) {
                        sum += bet
                    }
                    sum
                } else {
                    0
                }

            mainBetsTotal + sideBetsTotal + insuranceBet
        }

    /**
     * Net profit/loss for each hand: positive = win, negative = loss, zero = push.
     * Elements are null while the round is not yet terminal.
     * Bolt Performance Optimization: Pre-calculated once per state update.
     */
    val handNetPayouts: PersistentList<Int?> =
        run {
            if (!status.isTerminal()) return@run persistentListOf<Int?>()
            val results = mutableListOf<Int?>()
            for (i in playerHands.indices) {
                val hand = playerHands[i]
                val bet = hand.bet
                val payout = BlackjackRules.resolveHand(hand, bet, dealerHand.score, dealerHand.isBust, rules)
                val net = if (hand.isSurrendered) -(bet - bet / 2) else payout - bet
                results.add(net)
            }
            results.toPersistentList()
        }

    /**
     * Total net across all hands: sum of [handNetPayouts].
     * Null while the round is not yet terminal.
     * Bolt Performance Optimization: Pre-calculated once per state update.
     */
    val totalNetPayout: Int? =
        if (!status.isTerminal()) {
            null
        } else {
            var total = 0
            for (i in handNetPayouts.indices) {
                total += handNetPayouts[i] ?: 0
            }
            total
        }

    /** Returns the [Hand] corresponding to the [activeHandIndex]. */
    val activeHand: Hand get() = playerHands[activeHandIndex]

    /** Returns the current main bet for the [activeHand]. */
    val activeBet: Int get() = activeHand.bet

    /**
     * Returns true if the player can double down on their [activeHand].
     *
     * Valid when the hand has exactly 2 cards, sufficient balance exists, and the game rules
     * allow doubling in this state (e.g., after a split if [GameRules.allowDoubleAfterSplit] is true).
     * Domain predicate — keeps action-eligibility logic out of the UI layer.
     */
    val canDoubleDown: Boolean
        get() =
            activeHand.cards.size == 2 &&
                balance >= activeBet &&
                (!activeHand.wasSplit || rules.allowDoubleAfterSplit)

    /**
     * Returns true if the player can split their [activeHand].
     *
     * Valid when they have exactly 2 cards of equal rank (or value, if configured), sufficient
     * balance exists, and the total hand count has not reached [BlackjackConfig.MAX_HANDS].
     * Domain predicate — keeps action-eligibility logic out of the UI layer.
     */
    val canSplit: Boolean
        get() {
            if (playerHands.size >= BlackjackConfig.MAX_HANDS ||
                activeHand.cards.size != 2 ||
                balance < activeBet
            ) {
                return false
            }
            val c0 = activeHand.cards[0].rank
            val c1 = activeHand.cards[1].rank
            val rankMatch = if (rules.splitOnValueOnly) c0.value == c1.value else c0 == c1
            return rankMatch
        }

    /**
     * Returns true if the player can surrender their [activeHand].
     *
     * Valid at the start of a turn (exactly 2 cards) if the hand was not created via a split
     * and the house rules allow surrender. Surrendering forfeits half the bet.
     * Domain predicate — keeps action-eligibility logic out of the UI layer.
     */
    val canSurrender: Boolean
        get() = activeHand.cards.size == 2 && !activeHand.wasSplit && rules.allowSurrender

    /**
     * Returns true if the deal action is available during the betting phase:
     * at least one hand exists and every hand has a non-zero bet placed.
     * Domain predicate — keeps bet validation logic out of the UI layer.
     */
    val canDeal: Boolean get() = playerHands.isNotEmpty() && playerHands.all { it.bet > 0 }

    /**
     * Returns true if at least one player hand has a bet that can be cleared,
     * meaning the reset-bet action is available during the betting phase.
     * Domain predicate — keeps bet validation logic out of the UI layer.
     */
    val canResetBet: Boolean get() = playerHands.any { it.bet > 0 }

    /** True if the game ended with the player winning via at least one Blackjack. */
    val hasPlayerBlackjackWin: Boolean
        get() = status == GameStatus.PLAYER_WON && playerHands.any { it.isBlackjack }

    /** True if the game ended with the dealer winning because all player hands busted. */
    val hasPlayerBustLoss: Boolean
        get() = status == GameStatus.DEALER_WON && playerHands.all { it.isBust }

    /** The dealer score visible to the player given the current game phase.
     *  Returns full score during/after the dealer's turn; otherwise only the upcard score.
     *  Domain predicate — keeps phase-visibility logic out of the UI layer. */
    val dealerDisplayScore: Int
        get() =
            if (status == GameStatus.DEALER_TURN || status.isTerminal()) {
                dealerHand.score
            } else {
                dealerHand.visibleScore
            }
}

/**
 * Net profit/loss for a single hand: positive = win, negative = loss, zero = push.
 * Returns null while the round is not yet terminal.
 * Bolt Performance Optimization: Uses pre-calculated [GameState.handNetPayouts].
 */
fun GameState.handNetPayout(index: Int): Int? = handNetPayouts.getOrNull(index)

/**
 * Total net across all hands: sum of per-hand net payouts.
 * Returns null while the round is not yet terminal.
 * Bolt Performance Optimization: Uses pre-calculated [GameState.totalNetPayout].
 */
fun GameState.totalNetPayout(): Int? = totalNetPayout
