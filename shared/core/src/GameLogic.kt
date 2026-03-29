package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable

@Serializable
enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

@Serializable
enum class Rank(
    val value: Int
) {
    TWO(2),
    THREE(3),
    FOUR(4),
    FIVE(5),
    SIX(6),
    SEVEN(7),
    EIGHT(8),
    NINE(9),
    TEN(10),
    JACK(10),
    QUEEN(10),
    KING(10),
    ACE(11)
}

@Immutable
@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
    val isFaceDown: Boolean = false,
) {
    val isFaceUp: Boolean get() = !isFaceDown
}

/**
 * Represents a set of cards held by either the player or the dealer.
 *
 * Each hand tracks its own cards, associated bet, and lifecycle status (e.g., whether it
 * was split or stands). Blackjack scoring logic, including natural blackjacks and
 * ace-reduction, is encapsulated here.
 *
 * @property cards The [PersistentList] of cards held in this hand.
 * @property bet The current amount wagered on this hand (main bet).
 * @property lastBet The previous bet amount, used for "Repeat Bet" functionality.
 * @property isStanding True if the player has chosen to take no further cards for this hand.
 * @property wasSplit True if this hand was created by splitting a identical pair.
 * @property isFromSplitAce True if this hand was one of two created by splitting a pair of Aces.
 *           Special rules often apply to split Aces (e.g., only one card deal).
 * @property isSurrendered True if the player has surrendered the hand (losing half the bet).
 */
@Immutable
@Serializable
data class Hand(
    val cards: PersistentList<Card> = persistentListOf(),
    val bet: Int = 0,
    val lastBet: Int = 0,
    val isStanding: Boolean = false,
    val wasSplit: Boolean = false,
    val isFromSplitAce: Boolean = false,
    val isSurrendered: Boolean = false,
) {
    // Shared ace-reduction logic. Accepts boolean to optionally ignore face-down cards.
    private fun calculateScore(ignoreFaceDown: Boolean): Int {
        var s = 0
        var aces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            if (ignoreFaceDown && card.isFaceDown) continue
            s += card.rank.value
            if (card.rank == Rank.ACE) aces++
        }
        while (s > 21 && aces > 0) {
            s -= 10
            aces--
        }
        return s
    }

    /**
     * The total point value of all cards in the hand, with Aces counted as 11
     * where possible without exceeding 21.
     */
    val score: Int get() = calculateScore(ignoreFaceDown = false)

    /**
     * The point value of only face-up cards in the hand.
     *
     * This is primarily used for the dealer up-card display before the hole card is revealed.
     */
    val visibleScore: Int get() = calculateScore(ignoreFaceDown = true)

    /** True if the hand's [score] exceeds 21. */
    val isBust: Boolean get() = score > 21

    /**
     * True if this hand is a "natural" blackjack — exactly 2 cards totalling 21.
     *
     * Note: A hand totaling 21 after splitting or hitting is NOT a natural blackjack.
     */
    val isBlackjack: Boolean get() = cards.size == 2 && score == 21

    /**
     * True if at least one Ace is being counted as 11 (i.e. the hand is "soft").
     *
     * Note: iterates **all** cards, including face-down ones. Only call this after the
     * dealer's hole card has been revealed (e.g. inside [BlackjackRules.shouldDealerDraw]).
     */
    val isSoft: Boolean
        get() {
            var hasAce = false
            var hardScore = 0
            for (i in 0 until cards.size) {
                val card = cards[i]
                if (card.rank == Rank.ACE) {
                    hasAce = true
                    hardScore += 1
                } else {
                    hardScore += card.rank.value
                }
            }
            if (!hasAce) return false
            // A hand is soft when the score (with at least one Ace as 11) differs
            // from the hard score (all Aces as 1).
            return score != hardScore
        }
}

@Serializable
enum class BlackjackPayout(
    val numerator: Int,
    val denominator: Int
) {
    THREE_TO_TWO(3, 2),
    SIX_TO_FIVE(6, 5),
}

@Serializable
data class GameRules(
    val dealerHitsSoft17: Boolean = true,
    val allowDoubleAfterSplit: Boolean = true,
    val allowSurrender: Boolean = false,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val deckCount: Int = 6,
    /**
     * When true, any two cards with the same point value may be split (e.g. King + Jack).
     * When false (default), only cards of the exact same [Rank] may be split.
     */
    val splitOnValueOnly: Boolean = false,
)

/**
 * Represents the lifecycle stages of a single Blackjack round.
 *
 * The standard sequence is: [BETTING] -> [DEALING] -> [PLAYING] -> [DEALER_TURN] -> terminal status.
 * Specialized states like [INSURANCE_OFFERED] may occur immediately after [DEALING].
 */
@Serializable
enum class GameStatus {
    /** The player is currently placing bets and configuring the game. No cards have been dealt. */
    BETTING,

    /** Transient state while cards are being dealt with animations. Always transitions to the next phase. */
    DEALING,

    /** Default uninitialized state, used as a placeholder before a game session starts. */
    IDLE,

    /** The round is active and the player is making decisions (Hit, Stand, etc.) for their hands. */
    PLAYING,

    /** Specialized state occurring if the dealer shows an Ace, offering the player an insurance side-bet. */
    INSURANCE_OFFERED,

    /** The dealer is revealing their hole card and drawing cards according to house rules. */
    DEALER_TURN,

    /** Terminal state: The player won the round (at least one hand won or had a natural Blackjack). */
    PLAYER_WON,

    /** Terminal state: The dealer won the round (all player hands lost or busted). */
    DEALER_WON,

    /** Terminal state: The round ended in a tie (all hands pushed or tie was the best outcome). */
    PUSH
}

fun GameStatus.isTerminal() = this == GameStatus.PLAYER_WON || this == GameStatus.DEALER_WON || this == GameStatus.PUSH

@Serializable
enum class SideBetType {
    TWENTY_ONE_PLUS_THREE,
    PERFECT_PAIRS
}

@Immutable
@Serializable
data class SideBetResult(
    val type: SideBetType,
    val payoutMultiplier: Int,
    val payoutAmount: Int,
    // e.g., "Flush", "Perfect Pair"
    val outcomeName: String
)

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
    val balance: Int = 1000,
    val insuranceBet: Int = 0,
    val sideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val sideBetResults: PersistentMap<SideBetType, SideBetResult> = persistentMapOf(),
    val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val rules: GameRules = GameRules(),
    val dealerDrawIsCritical: Boolean = false,
) {
    companion object {
        /**
         * Maximum number of hands reachable via splits (up to 4).
         * The initial deal is capped at 3 by `handleSelectHandCount` — the 4th hand
         * is only ever reachable by splitting an existing hand during play.
         */
        const val MAX_HANDS = 4
    }

    /**
     * Alias for the bet on the first hand. Used for backward compatibility with single-hand
     * systems (like insurance and certain result calculations).
     */
    val currentBet: Int get() = playerHands.getOrNull(0)?.bet ?: 0

    /**
     * Returns the total amount currently wagered on the table across all player hands,
     * active side bets, and insurance. Once side bets are settled ([sideBetResults] is not empty),
     * they are no longer counted towards the total table bet.
     */
    val totalBet: Int
        get() {
            // Bolt Performance Optimization: Replace .fold with indexed loop to avoid Iterator allocation
            var mainBetsTotal = 0
            for (i in 0 until playerHands.size) {
                mainBetsTotal += playerHands[i].bet
            }

            // Only count active side bets (before they are settled)
            val sideBetsTotal =
                if (sideBetResults.isEmpty()) {
                    var sum = 0
                    for ((_, bet) in sideBets) {
                        sum += bet
                    }
                    sum
                } else {
                    0
                }

            return mainBetsTotal + sideBetsTotal + insuranceBet
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
     */
    fun canDoubleDown(): Boolean =
        activeHand.cards.size == 2 &&
            balance >= activeBet &&
            (!activeHand.wasSplit || rules.allowDoubleAfterSplit)

    /**
     * Returns true if the player can split their [activeHand].
     *
     * Valid when they have exactly 2 cards of equal rank (or value, if configured), sufficient
     * balance exists, and the total hand count has not reached [MAX_HANDS].
     */
    fun canSplit(): Boolean {
        if (playerHands.size >= MAX_HANDS || activeHand.cards.size != 2 || balance < activeBet) return false
        val c0 = activeHand.cards[0].rank
        val c1 = activeHand.cards[1].rank
        val rankMatch = if (rules.splitOnValueOnly) c0.value == c1.value else c0 == c1
        return rankMatch
    }
}

enum class HandOutcome { NATURAL_WIN, WIN, PUSH, LOSS }

// Domain layer: per-hand and total net payout helpers so UI never re-implements bet math.

/** Net profit/loss for a single hand: positive = win, negative = loss, zero = push.
 * Returns null while the round is not yet terminal. */
fun GameState.handNetPayout(index: Int): Int? {
    if (!status.isTerminal()) return null
    val hand = playerHands.getOrNull(index) ?: return null
    val bet = hand.bet
    val payout = BlackjackRules.resolveHand(hand, bet, dealerHand.score, dealerHand.isBust, rules)
    return payout - bet
}

/** Total net across all hands: sum of per-hand net payouts.
 * Returns null while the round is not yet terminal. */
fun GameState.totalNetPayout(): Int? {
    if (!status.isTerminal()) return null
    var total = 0
    for (i in 0 until playerHands.size) {
        total += handNetPayout(i) ?: 0
    }
    return total
}

/**
 * Aggregated result of all player hands at the end of a round.
 * @param totalPayout sum of individual hand payouts (0 means all were lost)
 * @param anyWin true if at least one hand won or had a natural BJ
 * @param allPush true if every hand pushed (used to determine [GameStatus.PUSH])
 */
data class HandResults(
    val totalPayout: Int,
    val anyWin: Boolean,
    val allPush: Boolean
)

object BlackjackRules {
    const val BLACKJACK_SCORE = 21
    const val DEALER_STAND_THRESHOLD = 17
    const val DEALER_STIFF_MIN = 12
    const val CARDS_PER_DECK = 52
    const val RESHUFFLE_THRESHOLD_DIVISOR = 4

    fun shouldDealerDraw(
        hand: Hand,
        rules: GameRules
    ): Boolean {
        if (hand.score < DEALER_STAND_THRESHOLD) return true
        if (hand.score == DEALER_STAND_THRESHOLD && rules.dealerHitsSoft17 && hand.isSoft) return true
        return false
    }

    fun determineHandOutcome(
        hand: Hand,
        dealerScore: Int,
        dealerBust: Boolean
    ): HandOutcome {
        if (hand.isBust) return HandOutcome.LOSS
        val isNaturalBJ = hand.cards.size == 2 && hand.score == 21 && !hand.wasSplit
        return when {
            isNaturalBJ && dealerScore != 21 -> HandOutcome.NATURAL_WIN
            dealerBust || hand.score > dealerScore -> HandOutcome.WIN
            hand.score == dealerScore -> HandOutcome.PUSH
            else -> HandOutcome.LOSS
        }
    }

    /**
     * Returns the chip payout for a single hand.
     *
     * Natural BJ payout uses integer division (casino convention): e.g. with 3:2,
     * a $3 bet returns $7 (not $7.50). Odd-bet rounding is intentional and tested.
     */
    fun resolveHand(
        hand: Hand,
        bet: Int,
        dealerScore: Int,
        dealerBust: Boolean,
        rules: GameRules,
    ): Int {
        // Surrendered hands were already refunded at surrender time; return 0 here.
        if (hand.isSurrendered) return 0
        return when (determineHandOutcome(hand, dealerScore, dealerBust)) {
            HandOutcome.NATURAL_WIN -> bet + (bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
            HandOutcome.WIN -> bet * 2
            HandOutcome.PUSH -> bet
            HandOutcome.LOSS -> 0
        }
    }

    fun calculateHandResults(
        state: GameState,
        dealerScore: Int,
        dealerBust: Boolean,
    ): HandResults {
        var totalPayout = 0
        var anyWin = false
        var allPush = true
        for (i in state.playerHands.indices) {
            val hand = state.playerHands[i]
            val bet = hand.bet
            totalPayout += resolveHand(hand, bet, dealerScore, dealerBust, state.rules)
            if (hand.isSurrendered) {
                // Surrendered hands are not wins or pushes; payout was already refunded at surrender time.
                allPush = false
                continue
            }
            // Delegate to determineHandOutcome so NATURAL_WIN is correctly distinguished
            // from a regular WIN (fixes: natural BJ vs dealer-21 was incorrectly anyWin=true).
            val outcome = determineHandOutcome(hand, dealerScore, dealerBust)
            if (outcome == HandOutcome.NATURAL_WIN || outcome == HandOutcome.WIN) anyWin = true
            if (outcome != HandOutcome.PUSH) allPush = false
        }
        return HandResults(totalPayout, anyWin, allPush)
    }

    /**
     * Determines the game status immediately after the initial deal.
     *
     * **Multi-hand note:** if the dealer has a natural Blackjack the round ends as
     * [GameStatus.DEALER_WON] for all player hands — even if an individual player hand
     * is also a natural BJ. This is an intentional simplification for the multi-hand
     * flow. Single-hand correctly resolves the BJ-vs-BJ case as [GameStatus.PUSH].
     */
    fun determineInitialStatus(
        hands: List<Hand>,
        dealerHand: Hand,
        handCount: Int
    ): GameStatus {
        val dealerHasBJ = dealerHand.score == BLACKJACK_SCORE && dealerHand.cards.size == 2

        if (handCount == 1) {
            val playerHasBJ = hands[0].score == BLACKJACK_SCORE && hands[0].cards.size == 2
            return when {
                playerHasBJ && dealerHasBJ -> GameStatus.PUSH
                playerHasBJ -> GameStatus.PLAYER_WON
                dealerHasBJ -> GameStatus.DEALER_WON
                else -> GameStatus.PLAYING
            }
        }

        // Multi-hand: if dealer has Blackjack, all hands lose immediately.
        // Individual player naturals are not compared in this path.
        if (dealerHasBJ) return GameStatus.DEALER_WON

        return GameStatus.PLAYING
    }

    /**
     * Computes the post-deal status, final dealer hand, and balance delta.
     *
     * **Insurance invariant:** insurance is only offered when the dealer's up-card is an Ace
     * AND the player does NOT have a natural BJ (see `shouldOfferInsurance`). Therefore
     * [GameState.insuranceBet] is guaranteed to be 0 at this point — no refund is needed here.
     */
    fun resolveInitialOutcomeValues(
        current: GameState,
        playerHands: List<Hand>,
        dealerHand: Hand,
    ): Triple<GameStatus, Hand, Int> {
        val playerHasBJ =
            current.handCount == 1 && playerHands[0].score == BLACKJACK_SCORE && playerHands[0].cards.size == 2
        val shouldOfferInsurance = current.handCount == 1 && !playerHasBJ && dealerHand.cards[0].rank == Rank.ACE
        // Bolt Performance Optimization: Prevent reallocation of already face-up cards to preserve reference equality.
        val dealerHandRevealed =
            Hand(dealerHand.cards.map { if (it.isFaceDown) it.copy(isFaceDown = false) else it }.toPersistentList())

        val initialStatus =
            if (shouldOfferInsurance) {
                GameStatus.INSURANCE_OFFERED
            } else {
                determineInitialStatus(playerHands, dealerHandRevealed, current.handCount)
            }

        val finalDealerHand =
            if (initialStatus == GameStatus.PUSH || initialStatus == GameStatus.DEALER_WON) {
                dealerHandRevealed
            } else {
                dealerHand
            }

        val balanceUpdate =
            when (initialStatus) {
                GameStatus.PLAYER_WON ->
                    (current.currentBet * current.rules.blackjackPayout.numerator) /
                        current.rules.blackjackPayout.denominator + current.currentBet
                GameStatus.PUSH -> current.currentBet
                else -> 0
            }

        return Triple(initialStatus, finalDealerHand, balanceUpdate)
    }

    fun createDeck(
        rules: GameRules,
        random: kotlin.random.Random
    ): List<Card> {
        val deckSize = rules.deckCount * CARDS_PER_DECK
        val newDeck = ArrayList<Card>(deckSize)
        val suits = Suit.entries
        val ranks = Rank.entries
        for (d in 1..rules.deckCount) {
            for (i in 0 until suits.size) {
                val suit = suits[i]
                for (j in 0 until ranks.size) {
                    newDeck.add(Card(ranks[j], suit))
                }
            }
        }
        newDeck.shuffle(random)
        return newDeck
    }
}

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

    /** Resets all main bets on the table back to the player's balance. Only valid during [GameStatus.BETTING]. */
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
}

sealed class GameEffect {
    data object PlayCardSound : GameEffect()

    data object PlayWinSound : GameEffect()

    data object PlayLoseSound : GameEffect()

    data object Vibrate : GameEffect()

    data object DealerCriticalDraw : GameEffect()

    data class NearMissHighlight(
        val handIndex: Int
    ) : GameEffect()

    data object HeavyCardThud : GameEffect()

    data object Pulse21 : GameEffect()

    data object LightTick : GameEffect()

    data object WinPulse : GameEffect()

    data object BustThud : GameEffect()

    data class ChipEruption(
        val amount: Int,
        val sideBetType: SideBetType? = null
    ) : GameEffect()

    data class ChipLoss(
        val amount: Int
    ) : GameEffect()

    data object PlayPlinkSound : GameEffect()

    data object PlayPushSound : GameEffect()
}
