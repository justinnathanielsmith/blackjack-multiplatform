package io.github.smithjustinn.blackjack.infra
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

import platform.posix.arc4random
import kotlin.random.Random

actual val secureRandom: Random =
    object : Random() {
        private const val INT_BITS = 32

        override fun nextBits(bitCount: Int): Int {
            if (bitCount <= 0) return 0
            // arc4random returns a 32-bit unsigned integer.
            // We shift it to get the requested number of bits.
            return (arc4random().toInt() ushr (INT_BITS - bitCount))
        }
    }
