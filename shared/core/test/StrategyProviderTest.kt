package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertEquals

class StrategyProviderTest {

    @Test
    fun testGetHardStrategy_hasCorrectSizeAndContent() {
        val strategy = StrategyProvider.getHardStrategy()

        assertEquals(10, strategy.size)

        // 17+
        assertEquals("17+", strategy[0].playerValue)
        assertEquals(10, strategy[0].actions.size)
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.STAND, strategy[0].actions[dealerUpcard])
        }

        // 11
        val elevenStrategy = strategy.first { it.playerValue == "11" }
        (2..10).forEach { dealerUpcard ->
            assertEquals(StrategyAction.DOUBLE, elevenStrategy.actions[dealerUpcard])
        }
        assertEquals(StrategyAction.HIT, elevenStrategy.actions[11])

        // 8 or less
        val eightOrLessStrategy = strategy.first { it.playerValue == "8 or less" }
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.HIT, eightOrLessStrategy.actions[dealerUpcard])
        }
    }

    @Test
    fun testGetSoftStrategy_hasCorrectSizeAndContent() {
        val strategy = StrategyProvider.getSoftStrategy()

        assertEquals(8, strategy.size)

        // A,9
        assertEquals("A,9", strategy[0].playerValue)
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.STAND, strategy[0].actions[dealerUpcard])
        }

        // A,8
        val a8Strategy = strategy.first { it.playerValue == "A,8" }
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.STAND, a8Strategy.actions[dealerUpcard])
        }

        // A,7
        val a7Strategy = strategy.first { it.playerValue == "A,7" }
        assertEquals(StrategyAction.STAND, a7Strategy.actions[2])
        (3..6).forEach { dealerUpcard ->
            assertEquals(StrategyAction.DOUBLE, a7Strategy.actions[dealerUpcard])
        }
        assertEquals(StrategyAction.STAND, a7Strategy.actions[7])
        assertEquals(StrategyAction.STAND, a7Strategy.actions[8])
        (9..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.HIT, a7Strategy.actions[dealerUpcard])
        }
    }

    @Test
    fun testGetPairsStrategy_hasCorrectSizeAndContent() {
        val strategy = StrategyProvider.getPairsStrategy()

        assertEquals(10, strategy.size)

        // A,A
        val aaStrategy = strategy.first { it.playerValue == "A,A" }
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.SPLIT, aaStrategy.actions[dealerUpcard])
        }

        // 10,10
        val tenTenStrategy = strategy.first { it.playerValue == "10,10" }
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.STAND, tenTenStrategy.actions[dealerUpcard])
        }

        // 8,8
        val eightEightStrategy = strategy.first { it.playerValue == "8,8" }
        (2..11).forEach { dealerUpcard ->
            assertEquals(StrategyAction.SPLIT, eightEightStrategy.actions[dealerUpcard])
        }
    }
}
