# Chip Tray Balance Reflection & Animation Optimization Plan

## Objective
1. **Chip Rack**: Update the `ChipRack` component so that it visually reflects the physical amount of chips the player has in their balance by breaking down their total balance into specific chip denominations and displaying stacks of chips. *(Already Implemented)*
2. **Animation Performance**: Improve the performance of chip animations by reusing chips instead of re-instantiating them. Specifically, implement an Object Pool for flying chip animations to reuse chips that are already allocated ("on the board"/in memory) to prevent heavy Garbage Collection overhead and Compose node reallocation.

## Approach

### Phase 1: Object Pooling for Flying Chips
1. **State Class Modification (`FlyingChipEffect.kt` / `BettingPhaseScreen.kt`)**:
   - Change `FlyingChip` from an immutable `data class` to a state-holder class `FlyingChipState` (or similar) with `MutableState` properties for `isActive`, `startOffset`, `targetOffset`, `amount`, `color`, and `textColor`.
2. **Pool Initialization**:
   - In `BettingPhaseScreen`, replace `mutableStateListOf<FlyingChip>()` with a pre-allocated pool of chips: `val flyingChipPool = remember { List(15) { FlyingChipState(it.toLong()) } }`.
3. **Reusing Chips**:
   - In `launchChip()`, instead of instantiating and adding a new chip to a list, find the first inactive chip in the pool (`flyingChipPool.firstOrNull { !it.isActive }`).
   - Update its properties (`startOffset`, `amount`, etc.) and set `isActive = true`.
4. **Animation Rendering**:
   - Iterate over the pool: `flyingChipPool.forEach { chip -> if (chip.isActive) { ... } }`.
   - On animation end, simply set `chip.isActive = false` instead of removing it from a list.

### Phase 2: Optimizing Particle Effects (Optional/Secondary)
- If particle effects (`ChipLossEffect`, `ChipEruptionEffect`) are causing jank, ensure they use efficient list operations or similar pooling techniques (they currently use `removeAll`, which is O(N), but pre-allocating an array of reusable particles can further eliminate allocations).

## Files to Modify
- `sharedUI/src/ui/screens/BettingPhaseScreen.kt`: Replace dynamic list with a fixed-size pool of reusable chip states.
- `sharedUI/src/ui/effects/FlyingChipEffect.kt`: Adapt the `FlyingChip` model to support mutable, reusable state.

## Verification
- Place rapid bets in the betting phase and ensure the chip tossing animation remains fluid (60/120fps) without stuttering or GC pauses.
- Verify that chips reset their states correctly when reused from the pool.