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
 * The immutable single source of truth for the Blackjack game engine.
 *
 * This state coordinates the lifecycle of a round ([GameStatus]), the physical deck,
 * and all active hands. It is the core input for the [BlackjackStateMachine] and
 * determines UI visibility via domain guards.
 *
 * **Invariants:**
 * - [playerHands] size is always ≥ 1.
 * - [activeHandIndex] is 0-indexed for player hands.
 * - [activeHandIndex] == -1 is a sentinel value indicating the dealer is the active participant.
 * - Modifications must occur via `copy()` and are orchestrated by state machine reducers.
 *
 * @property deck The persistent shoe of cards. Not revealed to the UI except for visual counts.
 * @property playerHands All active hands, including those born from splits (max [BlackjackConfig.MAX_HANDS]).
 * @property activeHandIndex 0-indexed pointer to the hand currently awaiting action. -1 if dealer turn.
 * @property handCount Initial number of hands (1-3) at round start. Used for repeat-bet anchoring.
 * @property dealerHand The dealer's current cards. Hole card visibility is gated by [status].
 * @property status The operational phase. Governs valid [io.github.smithjustinn.blackjack.action.GameAction]s.
 * @property balance Total player liquidity. Active bets are already deducted from this value.
 * @property insuranceBet Secondary wager placed only when [GameStatus.INSURANCE_OFFERED].
 * @property sideBets Active side-bet wagers, fixed from the [GameStatus.BETTING] phase.
 * @property sideBetResults Immutable results calculated immediately after the initial deal.
 * @property lastSideBets Persistence anchor for "repeat side bet" functionality.
 * @property lastBets Persistence anchor for "repeat main bet" across multiple hands.
 * @property rules The house rules (H17/S17, payouts, splitting) governing this session.
 * @property dealerDrawIsCritical Invariant guard: true if the dealer's next draw could bust or hit a threshold.
 * @property handOutcomes terminal result mapping (Win/Loss/Push) for each index in [playerHands].
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
     * Domain guard: True if the player can double down on their [activeHand].
     *
     * Valid when the hand has exactly 2 cards, sufficient balance exists, and the game rules
     * allow doubling in this state (e.g., after a split if [GameRules.allowDoubleAfterSplit] is true).
     */
    val canDoubleDown: Boolean
        get() =
            activeHand.cards.size == 2 &&
                balance >= activeBet &&
                (!activeHand.wasSplit || rules.allowDoubleAfterSplit)

    /**
     * Domain guard: True if the player can split their [activeHand].
     *
     * Valid when they have exactly 2 cards of equal rank (or value, if configured), sufficient
     * balance exists, and the total hand count has not reached [BlackjackConfig.MAX_HANDS].
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
     * Domain guard: True if the player can surrender their [activeHand].
     *
     * Valid at the start of a turn (exactly 2 cards) if the hand was not created via a split
     * and the house rules allow surrender. Surrendering forfeits half the bet.
     */
    val canSurrender: Boolean
        get() = activeHand.cards.size == 2 && !activeHand.wasSplit && rules.allowSurrender

    /**
     * Domain guard: True if the deal action is available during the betting phase.
     * Every enabled hand must have a non-zero bet placed.
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

    /**
     * Domain guard: The dealer score visible to the player given the current game phase.
     * Returns full score during/after the dealer's turn; otherwise only the upcard score.
     */
    val dealerDisplayScore: Int
        get() =
            if (status == GameStatus.DEALER_TURN || status.isTerminal()) {
                dealerHand.score
            } else {
                dealerHand.visibleScore
            }

    // ── Domain Guards: HUD Visibility — keeps phase-gating logic out of Composables ──

    /** True when the player is in the betting phase (no cards dealt yet). */
    val isBettingPhase: Boolean get() = status == GameStatus.BETTING

    /** True when the dealer's hole card has been revealed (dealer turn or round over).
     *  Used to gate dealer score/status display in the HUD. */
    val isDealerFullyRevealed: Boolean
        get() = status == GameStatus.DEALER_TURN || status.isTerminal()

    /** True if the dealer bust is visible to the player (hole card revealed + dealer is bust). */
    val isDealerBustVisible: Boolean get() = isDealerFullyRevealed && dealerHand.isBust

    /** True if the dealer has 21 and that score is visible to the player. */
    val isDealer21Visible: Boolean get() = isDealerFullyRevealed && dealerHand.isScore21

    /** True when the player turn is active (cards dealt, awaiting player action). */
    val isPlayingPhase: Boolean get() = status == GameStatus.PLAYING

    /**
     * Domain guard: True when the dealer is the currently-active "hand" during the play phase.
     * The dealer uses sentinel [activeHandIndex] == -1.
     */
    val isDealerActive: Boolean get() = isPlayingPhase && activeHandIndex == -1

    /** True when the player hand at [index] is the currently-active hand. */
    fun isHandActive(index: Int): Boolean = isPlayingPhase && index == activeHandIndex
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
