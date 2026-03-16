package io.github.smithjustinn.blackjack.utils

import kotlin.random.Random
import platform.posix.arc4random

actual val secureRandom: Random = object : Random() {
    override fun nextBits(bitCount: Int): Int {
        if (bitCount <= 0) return 0
        // arc4random returns a 32-bit unsigned integer.
        // We shift it to get the requested number of bits.
        return (arc4random().toInt() ushr (32 - bitCount))
    }
}
