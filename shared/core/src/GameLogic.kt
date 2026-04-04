package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Represents the four standard suits in a deck of playing cards.
 */
@Serializable
enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES
}

/**
 * Represents the rank of a playing card and its associated scoring value in Blackjack.
 *
 * Face cards (Jack, Queen, King) are worth 10. Aces are initially worth 11, subject
 * to value reduction to 1 during [Hand] score calculation if the total would otherwise bust.
 *
 * @property value The base Blackjack scoring value for this rank.
 */
@Serializable
enum class Rank(
    /** The base Blackjack scoring value for this rank. */
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

/**
 * Represents a single playing card with a specific [rank] and [suit].
 *
 * In this application, a card also carries a [isFaceDown] state, which determines
 * its visibility in the UI and whether its score is included in [Hand.visibleScore].
 *
 * @property rank The [Rank] of the card (e.g., ACE, KING, TWO).
 * @property suit The [Suit] of the card (e.g., HEARTS, SPADES).
 * @property isFaceDown If true, the card's rank and suit are hidden from the player.
 */
@Immutable
@Serializable
data class Card(
    val rank: Rank,
    val suit: Suit,
    val isFaceDown: Boolean = false,
) {
    /** True if the card is face-up (visible to players). */
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
    /** True when this hand was doubled down — the 3rd card should render rotated 90°. */
    val isDoubleDown: Boolean = false,
) {
    private data class HandMetrics(
        val score: Int,
        val isSoft: Boolean
    )

    // Bolt Performance Optimization: Score and softness are computed in a single pass.
    // Memoization using `by lazy` reduces O(N) card iterations to O(1) during gameplay.
    private val metrics: HandMetrics by lazy {
        var s = 0
        var aces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            s += card.rank.value
            if (card.rank == Rank.ACE) aces++
        }
        while (s > 21 && aces > 0) {
            s -= 10
            aces--
        }
        HandMetrics(score = s, isSoft = aces > 0)
    }

    /**
     * The total point value of all cards in the hand, with Aces counted as 11
     * where possible without exceeding 21.
     */
    val score: Int get() = metrics.score

    /**
     * The point value of only face-up cards in the hand.
     * This is primarily used for the dealer up-card display before the hole card is revealed.
     */
    val visibleScore: Int by lazy {
        var vs = 0
        var vAces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            if (card.isFaceDown) continue
            vs += card.rank.value
            if (card.rank == Rank.ACE) vAces++
        }
        while (vs > 21 && vAces > 0) {
            vs -= 10
            vAces--
        }
        vs
    }

    /** True if the hand's [score] exceeds 21. */
    val isBust: Boolean by lazy { score > 21 }

    /**
     * True if this hand is a "natural" blackjack — exactly 2 cards totalling 21.
     * Note: A hand totaling 21 after splitting or hitting is NOT a natural blackjack.
     */
    val isBlackjack: Boolean by lazy { cards.size == 2 && score == 21 }

    /**
     * True if the hand totals 21 via three or more cards (not a natural blackjack).
     * Distinct from [isBlackjack], which requires exactly 2 cards.
     */
    val isTwentyOne: Boolean by lazy { score == 21 && !isBlackjack }

    /**
     * A [0.0, 1.0] weight representing how dangerous this hand is to hit.
     * Scores 17–20 are the standing-risk zone; scores below 17 carry no tension.
     */
    @Suppress("MagicNumber") // Blackjack standing-risk thresholds are self-documenting domain values
    val tension: Float by lazy {
        when {
            score >= 20 -> 1.0f
            score == 19 -> 0.7f
            score == 18 -> 0.4f
            score == 17 -> 0.2f
            else -> 0.0f
        }
    }

    /**
     * True if at least one Ace is being counted as 11 (i.e. the hand is "soft").
     * Derived efficiently from the score calculation result.
     */
    val isSoft: Boolean get() = metrics.isSoft
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

// Statuses during which the game result overlay is shown; complements isTerminal() for UI gating.
fun GameStatus.isStatusVisible() = this == GameStatus.DEALING || this == GameStatus.DEALER_TURN || this.isTerminal()

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
    val balance: Int = BlackjackConfig.INITIAL_BALANCE,
    val insuranceBet: Int = 0,
    val sideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val sideBetResults: PersistentMap<SideBetType, SideBetResult> = persistentMapOf(),
    val lastSideBets: PersistentMap<SideBetType, Int> = persistentMapOf(),
    val rules: GameRules = GameRules(),
    val dealerDrawIsCritical: Boolean = false,
) {
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
     * balance exists, and the total hand count has not reached [BlackjackConfig.MAX_HANDS].
     */
    fun canSplit(): Boolean {
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
     */
    fun canSurrender(): Boolean = activeHand.cards.size == 2 && !activeHand.wasSplit && rules.allowSurrender

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

    // Special case: Surrender already refunded half the bet.
    // resolveHand() returns 0 for surrendered hands because they are settled.
    // But the net payout should reflect the loss of half the bet.
    if (hand.isSurrendered) {
        return -(bet - bet / 2)
    }

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

/**
 * The central engine for Blackjack rules, scoring, and deal orchestration.
 *
 * This object contains pure logic for determining hand outcomes, calculating payouts,
 * and managing the lifecycle of a round from initial deal to final settlement.
 */
object BlackjackRules {
    /** The target score for a natural Blackjack or a non-bust hand. */
    const val BLACKJACK_SCORE = BlackjackConfig.BLACKJACK_SCORE

    /** The minimum score a dealer must achieve before they stop drawing (Stand). */
    const val DEALER_STAND_THRESHOLD = BlackjackConfig.DEALER_STAND_THRESHOLD

    /** The lowest score at which a dealer hand is considered "stiff" (risk of busting on next draw). */
    const val DEALER_STIFF_MIN = BlackjackConfig.DEALER_STIFF_MIN

    /** Total number of cards in a standard physical deck. */
    const val CARDS_PER_DECK = BlackjackConfig.CARDS_PER_DECK

    /** The divisor used to determine when the shoe should be reshuffled (e.g. 4 = reshuffle at 25% remaining). */
    const val RESHUFFLE_THRESHOLD_DIVISOR = BlackjackConfig.RESHUFFLE_THRESHOLD_DIVISOR

    /**
     * Returns true if the dealer must draw another card according to standard rules.
     *
     * Dealers typically stand on 17+, but may hit on "soft" 17 if [GameRules.dealerHitsSoft17] is enabled.
     *
     * @param hand The dealer's current [Hand].
     * @param rules The [GameRules] defining house hit/stand behavior.
     * @return True if the dealer should hit, false if they should stand.
     */
    fun shouldDealerDraw(
        hand: Hand,
        rules: GameRules
    ): Boolean {
        if (hand.score < DEALER_STAND_THRESHOLD) return true
        if (hand.score == DEALER_STAND_THRESHOLD && rules.dealerHitsSoft17 && hand.isSoft) return true
        return false
    }

    /**
     * Determines the win/loss status of a single player hand against the dealer's score.
     *
     * @param hand The player's [Hand] to evaluate.
     * @param dealerScore The final point total achieved by the dealer.
     * @param dealerBust True if the dealer exceeded 21.
     * @return The resulting [HandOutcome] for the player.
     */
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
     * Returns the chip payout for a single hand based on its outcome.
     *
     * Natural BJ payout uses integer division (casino convention): e.g. with 3:2,
     * a $3 bet returns $7 (not $7.50). Odd-bet rounding is intentional and tested.
     *
     * @param hand The [Hand] being settled.
     * @param bet The specific amount wagered on this hand.
     * @param dealerScore The final total for the dealer.
     * @param dealerBust True if the dealer busted.
     * @param rules The [GameRules] for this round (includes payout multipliers).
     * @return The total number of chips to return to the player (initial bet + profit).
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

    /**
     * Aggregates the results across all player hands at the end of a dealer turn.
     *
     * @param state The current [GameState] containing player hands and rules.
     * @param dealerScore The dealer's final point total.
     * @param dealerBust True if the dealer busted.
     * @return A [HandResults] object summarizing the total payout and broad win/push status.
     */
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
     * Determines the initial status of the game immediately following the deal.
     *
     * Handles instant wins (natural Blackjack) and ties. In multi-hand play, if the dealer
     * has a natural Blackjack, all user hands are considered lost immediately.
     *
     * @param hands The player cards dealt.
     * @param dealerHand The dealer's initial two cards.
     * @param handCount The number of active hands.
     * @return The starting [GameStatus] for the round.
     */
    fun determineInitialStatus(
        hands: List<Hand>,
        dealerHand: Hand,
    ): GameStatus {
        val dealerHasBJ = dealerHand.isBlackjack
        var allDealerWon = true
        var anyPush = false
        var anyPlayerWon = false
        var anyStillPlaying = false

        for (i in 0 until hands.size) {
            val hand = hands[i]
            val playerHasBJ = hand.isBlackjack
            when {
                playerHasBJ && dealerHasBJ -> {
                    anyPush = true
                    allDealerWon = false
                }
                playerHasBJ -> {
                    anyPlayerWon = true
                    allDealerWon = false
                }
                dealerHasBJ -> {
                    // This hand lost.
                }
                else -> {
                    anyStillPlaying = true
                    allDealerWon = false
                }
            }
        }

        return when {
            anyStillPlaying -> GameStatus.PLAYING
            anyPlayerWon -> GameStatus.PLAYER_WON
            anyPush -> GameStatus.PUSH
            allDealerWon -> GameStatus.DEALER_WON
            else -> GameStatus.PLAYING // Defensive
        }
    }

    /**
     * Calculates the starting variables for the game after the initial two cards are dealt.
     *
     * This orchestrates status transitions (like [GameStatus.INSURANCE_OFFERED]), dealer card
     * reveal, and initial balance updates for natural blackjacks.
     */
    @Suppress("CyclomaticComplexMethod")
    fun resolveInitialOutcomeValues(
        current: GameState,
        playerHands: List<Hand>,
        dealerHand: Hand,
    ): Triple<GameStatus, Hand, Int> {
        // Bolt Performance Optimization: Replace .any with indexed loop to avoid iterator allocation.
        var anyPlayerHasBJ = false
        for (i in 0 until playerHands.size) {
            if (playerHands[i].isBlackjack) {
                anyPlayerHasBJ = true
                break
            }
        }

        val shouldOfferInsurance = current.handCount == 1 && !anyPlayerHasBJ && dealerHand.cards[0].rank == Rank.ACE
        // Bolt Performance Optimization: Prevent reallocation of already face-up cards to preserve reference equality.
        val dealerHandRevealed =
            Hand(
                dealerHand.cards.mutate { builder ->
                    for (i in 0 until builder.size) {
                        val card = builder[i]
                        if (card.isFaceDown) {
                            builder[i] = card.copy(isFaceDown = false)
                        }
                    }
                }
            )

        val initialStatus =
            if (shouldOfferInsurance) {
                GameStatus.INSURANCE_OFFERED
            } else {
                determineInitialStatus(playerHands, dealerHandRevealed)
            }

        val finalDealerHand = if (initialStatus.isTerminal()) dealerHandRevealed else dealerHand

        var balanceOutcome = 0
        val dealerHasBJ = dealerHandRevealed.isBlackjack

        if (initialStatus.isTerminal() || anyPlayerHasBJ) {
            for (i in 0 until playerHands.size) {
                val hand = playerHands[i]
                if (hand.isBlackjack) {
                    if (dealerHasBJ) {
                        balanceOutcome += hand.bet
                    } else {
                        balanceOutcome += (hand.bet * current.rules.blackjackPayout.numerator) /
                            current.rules.blackjackPayout.denominator + hand.bet
                    }
                } else if (dealerHasBJ) {
                    // Hand lost, no payout.
                }
            }
        }

        return Triple(initialStatus, finalDealerHand, balanceOutcome)
    }

    /**
     * Creates a new randomized shoe of cards based on the specified rule set.
     *
     * @param rules The [GameRules] defining the number of decks to include.
     * @param random The [Random] generator used for shuffling.
     * @return A shuffled [List] of [Card]s.
     */
    fun createDeck(
        rules: GameRules,
        random: Random
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
