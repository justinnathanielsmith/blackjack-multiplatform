# Plan: Optimize UI Effects Performance and Fix GC Churn

## Objective
Code review and fix performance issues in `@sharedUI/src/ui/effects/**` to adhere to the project's "High-performance particles" standard (MemoryMatch). The primary goal is to eliminate continuous object allocations (GC churn) occurring on every frame during particle animations.

## Key Files & Context
- `sharedUI/src/ui/effects/ChipEruptionEffect.kt`
- `sharedUI/src/ui/effects/ChipLossEffect.kt`
- `sharedUI/src/ui/effects/ConfettiEffect.kt`
- `sharedUI/src/ui/effects/PayoutEffect.kt`
- `sharedUI/src/ui/effects/ChipVisuals.kt`

## Code Review Findings & Changes

1. **Eliminate Allocation in `ChipVisuals.kt`:**
   - **Issue:** `drawChipVisual` allocates a new `Stroke`, `PathEffect.dashPathEffect`, and `FloatArray` on *every single draw call* for *every single chip*. When scaling the chips, the `radius` changes per frame, exacerbating the allocations.
   - **Fix:** Pre-compute and cache the `Stroke`, `PathEffect.dashPathEffect`, and `FloatArray` for the standard chip radius (`24f`). Modify `drawChipVisual` to use the cached `Stroke` when `radius == 24f` to completely eliminate allocations during drawing.

2. **Fix Particle Scaling (`ChipEruptionEffect.kt`, `ChipLossEffect.kt`):**
   - **Issue:** Changing `radius` directly continuously breaks the ability to cache path effects.
   - **Fix:** Instead of passing a continuously changing `radius`, pass the standard `24f` radius to `drawChipVisual`. Apply the scaling using Compose's allocation-free `DrawScope.scale()` and `DrawScope.translate()` transformations.

3. **Optimize Animation Loops (`ChipEruptionEffect.kt`, `ChipLossEffect.kt`, `ConfettiEffect.kt`, `PayoutEffect.kt`):**
   - **Issue:** The animation loops inside `withFrameNanos` use O(N) operations like `chips.removeAll { it.isDone }` and standard `forEach` / `for (chip in chips)` loops which allocate `Iterator` objects on every single frame. This leads to continuous GC churn.
   - **Fix:** Replace standard `forEach` and `for (item in list)` loops with index-based `for (i in 0 until size)` loops to avoid `Iterator` allocations. Replace O(N) removal lambdas (`removeAll { it.isDone }`) with single-pass index-based `while` loops that update and conditionally `removeAt(i)` in one go.

4. **Optimize Math (`PayoutEffect.kt`):**
   - **Issue:** The easing function uses `Math.pow(..., 2.0).toFloat()` which forces boxing/unboxing from Double.
   - **Fix:** Replace `Math.pow` with raw Float multiplication `(val inv = -2f * progress + 2f; inv * inv)` to avoid Double conversion overhead and stay completely in `Float` math. Fix detekt styling issues around the `progress < 0.5f` condition.

## Verification & Testing
1. Launch the app and trigger various game states (Win, Lose, Confetti, Payout).
2. Verify visually that all particle effects (Eruption, Loss, Confetti, Payout) still animate and scale correctly.
3. Verify via code inspection that no Iterators or PathEffects are allocated inside `withFrameNanos` blocks.