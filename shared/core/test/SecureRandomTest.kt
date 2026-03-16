package io.github.smithjustinn.blackjack.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertNotEquals

class SecureRandomTest {
    @Test
    fun testSecureRandomGeneratesDifferentValues() {
        val first = secureRandom.nextInt()
        val second = secureRandom.nextInt()
        // While theoretically possible to be equal, it's extremely unlikely for 32-bit ints
        assertNotEquals(first, second, "SecureRandom should generate different values")
    }

    @Test
    fun testSecureRandomInRange() {
        for (i in 0 until 100) {
            val value = secureRandom.nextInt(1, 10)
            assertTrue(value in 1..9, "Value $value should be in range [1, 10)")
        }
    }
}
