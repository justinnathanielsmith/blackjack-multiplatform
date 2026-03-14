# Specification - Performance Optimization

## Goal
Improve the overall responsiveness and frame-rate stability of the Blackjack application by eliminating redundant calculations, optimizing high-frequency loops, and ensuring state stability for efficient Compose skipping.

## Functional Requirements

### FR-PERF-1: Background Gradient Caching
**File:** `BlackjackScreen.kt`
**Problem:** The `radialGradient` Brush is recreated on every recomposition of `BoxWithConstraints`.
**Requirement:** `remember` the `Brush` object keyed on the relevant colors.

### FR-PERF-2: `flashAlpha` State Read Deferral
**File:** `BlackjackScreen.kt`
**Problem:** `flashAlpha.value` is read in composition and passed to `BlackjackGameOverlay`.
**Requirement:** Read `flashAlpha.value` only during the draw phase (e.g., using `drawBehind` or `graphicsLayer`) to avoid recomposing the overlay when the flash animates.

### FR-PERF-3: Persistent Collections for State Stability
**File:** `GameLogic.kt`
**Problem:** Standard `List` causes Compose to treat `GameState` as unstable unless manually annotated.
**Requirement:** Use `PersistentList` from `kotlinx.collections.immutable` for `playerHands` and `playerBets`.

### FR-PERF-4: Particle Allocation Optimization
**File:** `ConfettiEffect.kt`
**Problem:** Particle burst creates a temporary `List` and adds to `ArrayList`.
**Requirement:** Populate the `ArrayList` directly to avoid intermediate list creation.

### FR-PERF-5: Header String Formatting
**File:** `Header.kt`
**Problem:** `formatWithCommas()` is called multiple times per frame during balance animation.
**Requirement:** `remember` the formatted string, updating it only when `animatedBalance` changes.

### FR-PERF-6: Enable Strong Skipping
**File:** `module.yaml`
**Problem:** Default stability rules are conservative.
**Requirement:** Enable `experimentalStrongSkipping` to allow skipping even when parameters are technically unstable but haven't changed.

### FR-PERF-7: Card Back Drawing Optimization
**File:** `PlayingCard.kt`
**Problem:** Nested loops in `onDrawBehind` for checkerboard create thousands of draw calls across multiple cards.
**Requirement:** Use `drawWithCache` to pre-build a `Path` or use a tiled `ShaderBrush` to draw the pattern in a single call.

## Non-Functional Requirements
- Maintain existing visual fidelity.
- No changes to game logic.
- Pass all lint checks and tests.
