package io.github.smithjustinn.blackjack.infra

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

/** @see secureRandom */
actual val secureRandom: Random = SecureRandom().asKotlinRandom()
