package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.infra.secureRandom
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.Hand
import io.github.smithjustinn.blackjack.model.HandOutcome
import io.github.smithjustinn.blackjack.model.HandResults
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.model.isTerminal
import kotlinx.collections.immutable.mutate
import kotlin.random.Random

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

        val playerHasNaturalBJ = hand.isBlackjack && !hand.wasSplit
        return when {
            playerHasNaturalBJ && dealerScore != BLACKJACK_SCORE -> HandOutcome.NATURAL_WIN
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
            val playerHasBJ = hands[i].isBlackjack
            when {
                playerHasBJ && dealerHasBJ -> {
                    anyPush = true
                    allDealerWon = false
                }
                playerHasBJ -> {
                    anyPlayerWon = true
                    allDealerWon = false
                }
                !dealerHasBJ -> {
                    anyStillPlaying = true
                    allDealerWon = false
                }
            }
        }

        return resolveFinalInitialStatus(anyStillPlaying, anyPlayerWon, anyPush, allDealerWon)
    }

    private fun resolveFinalInitialStatus(
        anyStillPlaying: Boolean,
        anyPlayerWon: Boolean,
        anyPush: Boolean,
        allDealerWon: Boolean,
    ): GameStatus {
        return when {
            anyStillPlaying -> GameStatus.PLAYING
            anyPlayerWon -> GameStatus.PLAYER_WON
            anyPush -> GameStatus.PUSH
            allDealerWon -> GameStatus.DEALER_WON
            else -> GameStatus.PLAYING
        }
    }

    /**
     * Calculates the starting variables for the game after the initial two cards are dealt.
     *
     * This orchestrates status transitions (like [GameStatus.INSURANCE_OFFERED]), dealer card
     * reveal, and initial balance updates for natural blackjacks.
     *
     * @param current The current [GameState].
     * @param playerHands The list of player [Hand]s.
     * @param dealerHand The dealer's [Hand].
     * @return A [Triple] containing:
     *         1. The updated [GameStatus] (e.g., INSURANCE_OFFERED, PLAYING, or a terminal state).
     *         2. The possibly revealed dealer [Hand].
     *         3. The net change to the player's balance.
     */
    fun resolveInitialOutcomeValues(
        current: GameState,
        playerHands: List<Hand>,
        dealerHand: Hand,
    ): Triple<GameStatus, Hand, Int> {
        val anyPlayerHasBJ = hasAnyBlackjack(playerHands)

        val shouldOfferInsurance = current.handCount == 1 && !anyPlayerHasBJ && dealerHand.cards[0].rank == Rank.ACE
        val dealerHandRevealed = revealHoleCard(dealerHand)

        val initialStatus =
            if (shouldOfferInsurance) {
                GameStatus.INSURANCE_OFFERED
            } else {
                determineInitialStatus(playerHands, dealerHandRevealed)
            }

        val finalDealerHand = if (initialStatus.isTerminal()) dealerHandRevealed else dealerHand
        val balanceOutcome =
            if (initialStatus.isTerminal() || anyPlayerHasBJ) {
                calculateInitialPayout(current, playerHands, dealerHandRevealed.isBlackjack)
            } else {
                0
            }

        return Triple(initialStatus, finalDealerHand, balanceOutcome)
    }

    private fun hasAnyBlackjack(playerHands: List<Hand>): Boolean {
        // Bolt Performance Optimization: Replace .any with indexed loop to avoid iterator allocation.
        for (i in 0 until playerHands.size) {
            if (playerHands[i].isBlackjack) return true
        }
        return false
    }

    private fun revealHoleCard(dealerHand: Hand): Hand {
        // Bolt Performance Optimization: Prevent reallocation of already face-up cards to preserve reference equality.
        return Hand(
            dealerHand.cards.mutate { builder ->
                for (i in 0 until builder.size) {
                    val card = builder[i]
                    if (card.isFaceDown) {
                        builder[i] = card.copy(isFaceDown = false)
                    }
                }
            }
        )
    }

    private fun calculateInitialPayout(
        current: GameState,
        playerHands: List<Hand>,
        dealerHasBJ: Boolean
    ): Int {
        var balanceOutcome = 0
        for (i in 0 until playerHands.size) {
            balanceOutcome += calculateSingleInitialPayout(playerHands[i], dealerHasBJ, current.rules)
        }
        return balanceOutcome
    }

    private fun calculateSingleInitialPayout(
        hand: Hand,
        dealerHasBJ: Boolean,
        rules: GameRules
    ): Int {
        if (!hand.isBlackjack) return 0
        return if (dealerHasBJ) {
            hand.bet
        } else {
            val profit = (hand.bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
            profit + hand.bet
        }
    }

    /**
     * Creates a new randomized shoe of cards based on the specified rule set using
     * the system's [secureRandom] provider.
     *
     * @param rules The [GameRules] defining the number of decks to include.
     * @return A cryptographically-secure shuffled [List] of [Card]s.
     */
    fun createDeck(rules: GameRules): List<Card> = createDeck(rules, secureRandom)

    /**
     * Creates a new randomized shoe of cards based on the specified rule set.
     *
     * @param rules The [GameRules] defining the number of decks to include.
     * @param random The [Random] generator used for shuffling.
     * @return A shuffled [List] of [Card]s.
     */
    internal fun createDeck(
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
