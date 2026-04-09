package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.util.*
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*

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
    fun testGetHardStrategy_hasCorrectSizeAndContent() {
        val strategy = StrategyProvider.getHardStrategy()

        assertEquals(10, strategy.size)

        // 17+
        assertEquals("17+", strategy[0].playerValue)
        assertEquals(10, strategy[0].actions.size)
        for (dealerUpcard in 2..11) {
            assertEquals(StrategyAction.STAND, strategy[0].actions[dealerUpcard])
        }

        // 11
        val elevenStrategy = strategy.first { it.playerValue == "11" }
        for (dealerUpcard in 2..10) {
            assertEquals(StrategyAction.DOUBLE, elevenStrategy.actions[dealerUpcard])
        }
        assertEquals(StrategyAction.HIT, elevenStrategy.actions[11])

        // 8 or less
        val eightOrLessStrategy = strategy.first { it.playerValue == "8 or less" }
        for (dealerUpcard in 2..11) {
            assertEquals(StrategyAction.HIT, eightOrLessStrategy.actions[dealerUpcard])
        }
    }

    @Test
    fun testGetSoftStrategy_hasCorrectSizeAndContent() {
        val strategy = StrategyProvider.getSoftStrategy()

        assertEquals(8, strategy.size)

        // A,9
        assertEquals("A,9", strategy[0].playerValue)
        for (dealerUpcard in 2..11) {
            assertEquals(StrategyAction.STAND, strategy[0].actions[dealerUpcard])
        }

        // A,8
        val a8Strategy = strategy.first { it.playerValue == "A,8" }
        for (dealerUpcard in 2..11) {
            if (dealerUpcard == 6) {
                assertEquals(StrategyAction.DOUBLE, a8Strategy.actions[dealerUpcard])
            } else {
                assertEquals(StrategyAction.STAND, a8Strategy.actions[dealerUpcard])
            }
        }

        // A,7
        val a7Strategy = strategy.first { it.playerValue == "A,7" }
        assertEquals(StrategyAction.STAND, a7Strategy.actions[2])
        for (dealerUpcard in 3..6) {
            assertEquals(StrategyAction.DOUBLE, a7Strategy.actions[dealerUpcard])
        }
        assertEquals(StrategyAction.STAND, a7Strategy.actions[7])
        assertEquals(StrategyAction.STAND, a7Strategy.actions[8])
        for (dealerUpcard in 9..11) {
            assertEquals(StrategyAction.HIT, a7Strategy.actions[dealerUpcard])
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

    @Test
    fun getPairsStrategy_returnsCorrectStrategyCells() {
        val strategy = StrategyProvider.getPairsStrategy()

        assertEquals(10, strategy.size)

        // A,A
        val aa = strategy.first { it.playerValue == "A,A" }
        assertEquals((2..11).associateWith { StrategyAction.SPLIT }, aa.actions)

        // 10,10
        val tenTen = strategy.first { it.playerValue == "10,10" }
        assertEquals((2..11).associateWith { StrategyAction.STAND }, tenTen.actions)

        // 9,9
        val nineNine = strategy.first { it.playerValue == "9,9" }
        val expectedNineNine =
            (2..6).associateWith { StrategyAction.SPLIT } +
                mapOf(
                    7 to StrategyAction.STAND,
                    8 to StrategyAction.SPLIT,
                    9 to StrategyAction.SPLIT,
                    10 to StrategyAction.STAND,
                    11 to StrategyAction.STAND
                )
        assertEquals(expectedNineNine, nineNine.actions)

        // 8,8
        val eightEight = strategy.first { it.playerValue == "8,8" }
        assertEquals((2..11).associateWith { StrategyAction.SPLIT }, eightEight.actions)

        // 7,7
        val sevenSeven = strategy.first { it.playerValue == "7,7" }
        val expectedSevenSeven =
            (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
        assertEquals(expectedSevenSeven, sevenSeven.actions)

        // 6,6
        val sixSix = strategy.first { it.playerValue == "6,6" }
        val expectedSixSix =
            (2..6).associateWith { StrategyAction.SPLIT } + (7..11).associateWith { StrategyAction.HIT }
        assertEquals(expectedSixSix, sixSix.actions)

        // 5,5
        val fiveFive = strategy.first { it.playerValue == "5,5" }
        val expectedFiveFive =
            (2..9).associateWith { StrategyAction.DOUBLE } + (10..11).associateWith { StrategyAction.HIT }
        assertEquals(expectedFiveFive, fiveFive.actions)

        // 4,4
        val fourFour = strategy.first { it.playerValue == "4,4" }
        val expectedFourFour =
            mapOf(
                2 to StrategyAction.HIT,
                3 to StrategyAction.HIT,
                4 to StrategyAction.HIT,
                5 to StrategyAction.SPLIT,
                6 to StrategyAction.SPLIT,
                7 to StrategyAction.HIT,
                8 to StrategyAction.HIT,
                9 to StrategyAction.HIT,
                10 to StrategyAction.HIT,
                11 to StrategyAction.HIT
            )
        assertEquals(expectedFourFour, fourFour.actions)

        // 3,3
        val threeThree = strategy.first { it.playerValue == "3,3" }
        val expectedThreeThree =
            (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
        assertEquals(expectedThreeThree, threeThree.actions)

        // 2,2
        val twoTwo = strategy.first { it.playerValue == "2,2" }
        val expectedTwoTwo =
            (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
        assertEquals(expectedTwoTwo, twoTwo.actions)
    }
}
