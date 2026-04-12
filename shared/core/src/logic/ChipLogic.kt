package io.github.smithjustinn.blackjack.logic

import io.github.smithjustinn.blackjack.model.BlackjackConfig

/**
 * Domain logic for chip-related calculations.
 *
 * This object handles the conversion between balance amounts and physical chip
 * representation, ensuring that chip mathematics are separated from the UI layer.
 */
@Suppress("MagicNumber")
object ChipLogic {
    /**
     * Breaks down a balance into counts of specific chip denominations.
     * Starts with the largest denominations first.
     *
     * @param balance The total amount to break down.
     * @param chipValues The allowed chip denominations to use.
     * @return A map of chip denomination to the number of chips required.
     */
    fun breakdownBalance(
        balance: Int,
        chipValues: List<Int> = BlackjackConfig.RACK_DENOMINATIONS
    ): Map<Int, Int> {
        var remaining = balance
        val breakdown = mutableMapOf<Int, Int>()
        for (value in chipValues.sortedDescending()) {
            val count = remaining / value
            if (count > 0) {
                breakdown[value] = count
            }
            remaining %= value
        }
        return breakdown
    }

    /**
     * Calculates the sequence of chips (bottom to top) to visually represent a stack.
     *
     * @param amount The total amount of the stack.
     * @param maxStackSize The maximum number of chips to return.
     * @return A list of chip denominations representing the stack.
     */
    fun calculateChipStack(
        amount: Int,
        maxStackSize: Int = 8
    ): List<Int> {
        val list = mutableListOf<Int>()
        var remaining = amount
        for (denom in BlackjackConfig.CHIP_DENOMINATIONS) {
            while (remaining >= denom && list.size < maxStackSize) {
                list.add(denom)
                remaining -= denom
            }
        }
        list.reverse()
        return list
    }
}
