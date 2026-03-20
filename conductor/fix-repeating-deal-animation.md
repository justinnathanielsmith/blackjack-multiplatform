# Objective
Fix the repeating deal animation bug and address architectural, performance, and maintainability issues in the card table layout logic.

# Root Cause Analysis
1. **Repeating Animations (OverlayCardTable.kt)**: Missing Compose `key`s in `forEach` loops and fragile `remember` blocks tied to `slot.card` cause `Animatable` instances to reset when new cards are added or when the dealer's hole card flips.
2. **Layout Scale Anti-Pattern (CardTablePositioner.kt)**: Baking the `1.1f` active scale multiplier directly into the coordinate and size math causes the entire cluster to jump instantly when the active hand changes.
3. **Simultaneous Animation Delays (CardTablePositioner.kt)**: The `animDelay` is calculated purely based on the local card index within a hand, causing all hands' first cards to deal concurrently.
4. **Performance & Magic Numbers (CardTablePositioner.kt)**: `Density` conversions are performed inside the `forEachIndexed` loop, and the file uses magic numbers instead of named constants.

# Implementation Steps

1. **Add Stable Keys (OverlayCardTable.kt)**:
   - Wrap the `PositionedCardItem` call inside the `forEach` loop with a `key(slot.handIndex, slot.cardIndex)` block.

2. **Decouple Animatable State from Card Value (OverlayCardTable.kt)**:
   - Inside `PositionedCardItem`, remove `slot.card` from the `remember` blocks for `currentX`, `currentY`, `currentScale`, and `currentRotation`. Initialize them once.

3. **Refactor Table Layout Math (CardTablePositioner.kt)**:
   - **Extract Constants**: Move magic numbers (`0.22f`, `0.72f`, `0.12f`, `0.06f`, `6f`, etc.) into a private `TableMetrics` object at the top of the file.
   - **Hoist Density Math**: Move `with(density) { Dimensions.Card.StandardWidth.toPx() }` and `6.dp.toPx()` outside the player hands loop to avoid recalculating them per hand.
   - **Remove Active Scale from Math**: Remove the `if (isActive && nPlayerHands > 1) cardScale * 1.1f else cardScale` logic from `computeTableLayout`. Always use the base `cardScale` for layout calculations.
   - **Fix Animation Delays**: Pass a `globalCardIndex` into `computeZone` so that `animDelay` is staggered sequentially across all hands on the table.
   - **Simplify Hole Card Logic**: Remove the explicit `isHoleCard = isDealer && index == 1` flag and rely purely on the domain model's `isFaceDown` state.

4. **Apply Active Scale in Compose**:
   - Apply the `1.1f` scale boost dynamically in the `@Composable` layer (e.g., using `Modifier.scale(animateFloatAsState(...).value)`) for the currently active hand to ensure a smooth, animated transition.

# Verification & Testing
- **Multi-Hand Play**: Hit on a hand and verify cards in subsequent hands do not animate from the shoe again.
- **Hole Card Reveal**: Verify the dealer's hole card scales and rotates in place without flying from the shoe.
- **Active Hand Transition**: Verify that switching the active hand animates the scale smoothly without causing the hand's base coordinates to snap or jump.
- **Initial Deal**: Verify that cards are dealt sequentially rather than all at once.