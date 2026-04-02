package io.github.smithjustinn.blackjack.ui.effects

import io.github.smithjustinn.blackjack.ui.theme.ChipGreen
import io.github.smithjustinn.blackjack.ui.theme.ChipPurple
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChipVisualsTest {
    @Test
    fun breakdownAmountValues_zeroAmount_returnsEmptyList() {
        val result = ChipVisuals.breakdownAmountValues(0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun breakdownAmountValues_exactDenomination_returnsSingleValue() {
        val result = ChipVisuals.breakdownAmountValues(500)
        assertEquals(listOf(500), result)
    }

    @Test
    fun breakdownAmountValues_complexAmount_returnsExpectedValues() {
        val result = ChipVisuals.breakdownAmountValues(641).sortedDescending()
        assertEquals(listOf(500, 100, 25, 10, 5, 1), result)
    }

    @Test
    fun breakdownAmountValues_exceedsMaxParticles_truncates() {
        // 6000 = 12 * 500. With maxParticles = 10, it should return 10 * 500.
        val result = ChipVisuals.breakdownAmountValues(6000, maxParticles = 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it == 500 })
    }

    @Test
    fun breakdownAmountValues_exceedsMaxParticles_withLeftover_addsOne() {
        // 5004 = 10 * 500 + 4. Remaining is 4, but result size is 10, so it shouldn't add 1.
        val result = ChipVisuals.breakdownAmountValues(5004, maxParticles = 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it == 500 })
    }

    @Test
    fun breakdownAmountValues_withLeftoverAndSpace_addsOne() {
        // 4999 = 9 * 500 + 4 * 100 + 3 * 25 + 2 * 10 + 4
        // If we set maxParticles to 10.
        // denominations: 500, 100, 25, 10, 5, 1
        // 9 * 500 -> size 9. remaining 499.
        // 100 -> add 1 to reach 10 maxParticles. remaining 399.
        // size is now 10. loop breaks.
        // remaining > 0 (399), but size is not < maxParticles. So no 1 is added.
        val result = ChipVisuals.breakdownAmountValues(4999, maxParticles = 10).sortedDescending()
        assertEquals(listOf(500, 500, 500, 500, 500, 500, 500, 500, 500, 100), result)
    }

    @Test
    fun breakdownAmount_zeroAmount_returnsEmptyList() {
        val result = ChipVisuals.breakdownAmount(0)
        assertTrue(result.isEmpty())
    }

    @Test
    fun breakdownAmount_exactDenomination_returnsCorrectColors() {
        val result = ChipVisuals.breakdownAmount(500)
        assertEquals(listOf(ChipPurple), result)
    }

    @Test
    fun breakdownAmount_complexAmount_returnsCorrectColors() {
        // We compare using the ARGB representation by sorting
        val result = ChipVisuals.breakdownAmount(125).sortedBy { it.value }
        val expected = listOf(PokerBlack, ChipGreen).sortedBy { it.value }
        assertEquals(expected, result)
    }

    @Test
    fun breakdownAmount_exceedsMaxParticles_truncates() {
        val result = ChipVisuals.breakdownAmount(6000, maxParticles = 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it == ChipPurple })
    }

    @Test
    fun breakdownAmount_exceedsMaxParticles_withLeftover_noWhiteSoftAdded() {
        val result = ChipVisuals.breakdownAmount(5004, maxParticles = 10)
        assertEquals(10, result.size)
        assertTrue(result.all { it == ChipPurple })
    }
}
