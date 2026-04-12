package io.github.smithjustinn.blackjack.logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChipLogicTest {
    @Test
    fun testBreakdownBalance_Standard() {
        val balance = 142
        val breakdown = ChipLogic.breakdownBalance(balance)

        // Rack denominations: 100, 25, 10, 5, 1
        assertEquals(1, breakdown[100])
        assertEquals(1, breakdown[25])
        assertEquals(1, breakdown[10])
        assertEquals(1, breakdown[5])
        assertEquals(2, breakdown[1])
        assertEquals(5, breakdown.size)
    }

    @Test
    fun testBreakdownBalance_Empty() {
        val breakdown = ChipLogic.breakdownBalance(0)
        assertTrue(breakdown.isEmpty())
    }

    @Test
    fun testCalculateChipStack_SmallAmount() {
        val amount = 8
        val stack = ChipLogic.calculateChipStack(amount)

        // 8 = 5 + 1 + 1 + 1
        // list.reverse() -> [1, 1, 1, 5]
        assertEquals(listOf(1, 1, 1, 5), stack)
    }

    @Test
    fun testCalculateChipStack_LargeAmount() {
        val amount = 1250
        val stack = ChipLogic.calculateChipStack(amount)

        // 500, 500, 100, 100, 50 -> 5 chips (fits in maxStackSize 8)
        // Reversed -> [50, 100, 100, 500, 500]
        assertEquals(listOf(50, 100, 100, 500, 500), stack)
    }

    @Test
    fun testCalculateChipStack_MaxStackSize() {
        val amount = 10000 // A lot of chips
        val maxStackSize = 5
        val stack = ChipLogic.calculateChipStack(amount, maxStackSize = maxStackSize)

        assertEquals(maxStackSize, stack.size)
        // 10000 = many 500s. Top 5 should be 500s.
        assertTrue(stack.all { it == 500 })
    }
}
