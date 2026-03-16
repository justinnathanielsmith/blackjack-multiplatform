package io.github.smithjustinn.blackjack

import kotlin.test.Test
import kotlin.test.assertEquals

class StrategyProviderTest {

    @Test
    fun getSoftStrategy_hasCorrectDimensions() {
        val strategy = StrategyProvider.getSoftStrategy()

        // Soft strategy covers "A,9" down to "A,2"
        assertEquals(8, strategy.size)

        strategy.forEach { cell ->
            // Dealer upcards from 2 to 11
            assertEquals(10, cell.actions.size)
            for (upcard in 2..11) {
                assert(cell.actions.containsKey(upcard))
            }
        }
    }

    @Test
    fun getSoftStrategy_hasCorrectSpecificValues() {
        val strategy = StrategyProvider.getSoftStrategy()

        // Spot-check A,9 vs 2
        val a9 = strategy.find { it.playerValue == "A,9" }
        assert(a9 != null)
        assertEquals(StrategyAction.STAND, a9!!.actions[2])

        // Spot-check A,7 vs 3
        val a7 = strategy.find { it.playerValue == "A,7" }
        assert(a7 != null)
        assertEquals(StrategyAction.DOUBLE, a7!!.actions[3])
    }
}
