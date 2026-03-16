package io.github.smithjustinn.blackjack.utils

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

actual val secureRandom: Random = SecureRandom().asKotlinRandom()
