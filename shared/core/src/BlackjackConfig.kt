package io.github.smithjustinn.blackjack

/**
 * Authoritative source for game configuration and tuning parameters.
 *
 * Centralizing these values ensures consistency across UI components,
 * state machine logic, and betting validation.
 */
object BlackjackConfig {
    /** Minimum allowed bet for a standard Blackjack hand. */
    const val MIN_BET = 10

    /** Maximum allowed bet (Table Limit) for a standard Blackjack hand. */
    const val MAX_BET = 500

    /** The initial chip value selected when the player first enters the betting phase. */
    const val DEFAULT_CHIP_AMOUNT = 10

    /** The starting balance for a new game session. */
    const val INITIAL_BALANCE = 1000

    /** Maximum number of hands reachable via splits. */
    const val MAX_HANDS = 4

    /** Minimum number of seats (hands) a player can choose to play initially. */
    const val MIN_INITIAL_HANDS = 1

    /** Maximum number of seats (hands) a player can choose to play initially. */
    const val MAX_INITIAL_HANDS = 3

    /** The target score for a natural Blackjack or a non-bust hand. */
    const val BLACKJACK_SCORE = 21

    /** The minimum score a dealer must achieve before they stop drawing (Stand). */
    const val DEALER_STAND_THRESHOLD = 17

    /** The lowest score at which a dealer hand is considered "stiff" (risk of busting on next draw). */
    const val DEALER_STIFF_MIN = 12

    /** Total number of cards in a standard physical deck. */
    const val CARDS_PER_DECK = 52

    /** The divisor used to determine when the shoe should be reshuffled (e.g. 4 = reshuffle at 25% remaining). */
    const val RESHUFFLE_THRESHOLD_DIVISOR = 4

    // Internal keys for navigation and UI state strings
    const val NAV_KEY_RULES = "rules"
    const val NAV_KEY_STRATEGY = "strategy"
    const val NAV_KEY_SETTINGS = "settings"
}
