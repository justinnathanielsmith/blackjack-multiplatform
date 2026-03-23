# Chip Tray Balance Reflection Plan

## Objective
Update the `ChipRack` component so that it visually reflects the physical amount of chips the player has in their balance by breaking down their total balance into specific chip denominations and displaying stacks of chips.

## Approach
1. **Balance Breakdown Algorithm**:
   - Implement a greedy algorithm `breakdownBalance(balance: Int, chipValues: List<Int>)` that returns a `Map<Int, Int>` representing the quantity of each chip denomination.
   - Example: `$137 = 1x$100, 1x$25, 1x$10, 0x$5, 2x$1`.

2. **Visual Stacking**:
   - Inside `ChipRack`, compute the breakdown whenever `balance` changes.
   - For each chip denomination in the tray, determine its `count` from the breakdown.
   - If `count > 0`, render a stack of `BetChip`s (up to a visual maximum, e.g., 5 chips) by layering them in a `Box` with a negative Y-axis `offset` (e.g., `-4.dp` per chip).
   - The top-most chip in the stack should handle the interaction (`DragTarget` and `onClick`). The underlying chips in the stack can have interactions disabled.

3. **Empty Slots (Zero Count)**:
   - If `count == 0`, render a single "ghosted" `BetChip` (e.g., with `alpha = 0.3f` or `0.5f`) to represent an empty slot.
   - Players can still interact with this empty slot (click or drag) if `balance >= value` (i.e., making change is permitted), but it visually conveys that they don't possess that specific chip.

4. **Layout Adjustments**:
   - Ensure the `ChipRack`'s inner `Box` or `Row` has sufficient vertical padding or height to accommodate the upward Y-offset of the chip stacks without clipping.

## Files to Modify
- `sharedUI/src/ui/components/ChipRack.kt`: Add the breakdown logic, stacking loop, ghosting modifier, and adjust padding/layout to prevent clipping.

## Verification
- Previews: Check `ChipRackPreview`, `ChipRackLimitedBalancePreview`, and `ChipRackSelectedPreview` to ensure stacks render correctly.
- Gameplay: Place bets and observe the chip stacks recalculating and updating as the balance changes. Ensure 0-count chips are ghosted but remain clickable if the user has sufficient overall balance.