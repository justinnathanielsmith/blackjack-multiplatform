package io.github.smithjustinn.blackjack.infra

import kotlin.random.Random

/**
 * A cryptographically-secure random number generator provided by the underlying platform.
 *
 * This generator is used for high-stakes game actions where fairness and unpredictability
 * are critical, such as shuffling the deck in [io.github.smithjustinn.blackjack.logic.BlackjackRules.createDeck].
 *
 * Implementation details:
 * - Android/JVM: Uses `java.security.SecureRandom`.
 * - iOS: Uses `arc4random`.
 */
expect val secureRandom: Random
