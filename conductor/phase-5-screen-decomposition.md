# Phase 5: Screen Decomposition Implementation Plan

## Objective
Break `BlackjackScreen.kt` into smaller, focused composables (`TableSurface`, `GameOverlay`, `BettingLayer`, and `OverlayLayer`) to establish strong recomposition boundaries, significantly reducing the file size to ~200 lines and eliminating unnecessary recompositions.

## Key Files & Context
- `sharedUI/src/ui/screens/BlackjackScreen.kt` (Target for refactoring)
- `sharedUI/src/ui/screens/TableSurface.kt` (New)
- `sharedUI/src/ui/screens/GameOverlay.kt` (New)
- `sharedUI/src/ui/screens/BettingLayer.kt` (New)
- `sharedUI/src/ui/screens/OverlayLayer.kt` (New)

## Implementation Steps

### 1. Create `TableSurface.kt`
- Extract the decorative table drawing logic currently located inside the `BoxWithConstraints` in `BlackjackScreen.kt`.
- This includes the `modifier.fillMaxSize().background(FeltDeepEdge).drawWithCache(...)` block.
- The composable will accept a `Modifier` but **no state dependencies**, ensuring it never recomposes during gameplay.
- Also extract the static "BLACKJACK PAYS 3 TO 2" and other felt text into this component or keep them tightly scoped.

### 2. Create `GameOverlay.kt`
- Move the existing private `BlackjackGameOverlay` and `SideBetResultsOverlay` from `BlackjackScreen.kt` into this new file.
- Make them internal/public composables.
- Parameter scoping: It will only accept primitive or stable state fields it needs (e.g., `status: GameStatus`, `playerHands: List<Hand>`, `netPayout: Int?`, `isBlackjack: Boolean`, `flashAlphaProvider`, `flashColorProvider`, `showStatus`). Do not pass the entire `GameState`.

### 3. Create `BettingLayer.kt`
- Create a new `BettingLayer` composable.
- Extract the `AnimatedVisibility` wrapper for `BettingPhaseScreen` into it.
- Extract the three `for` loops that render particle effects (`animState.chipEruptions`, `animState.chipLosses`, `animState.activePayouts`) into this component.
- Parameter scoping: Pass `state.status`, `selectedAmount`, `animState`, `component`, and `audioService`.

### 4. Create `OverlayLayer.kt`
- Create a new `OverlayLayer` composable.
- Extract the `AnimatedVisibility` wrappers for `SettingsOverlay`, `RulesOverlay`, and `StrategyGuideOverlay`.
- Parameter scoping: Pass `showSettings`, `showRules`, `showStrategy`, `appSettings`, and their respective dismiss/action callbacks.

### 5. Refactor `BlackjackScreen.kt`
- Replace the extracted blocks in `BlackjackScreen` with calls to `TableSurface`, `GameOverlay`, `BettingLayer`, and `OverlayLayer`.
- Verify that `BlackjackScreen.kt` uses the recomposition boundaries correctly and drops below ~200 lines.
- Ensure the visual z-ordering is preserved (`TableSurface` at bottom, gameplay in middle, `GameOverlay` above gameplay, `BettingLayer` above that, `OverlayLayer` at the very top).

## Verification & Testing
- **Visual Parity**: Verify the UI looks strictly identical to the pre-refactor state.
- **Recomposition Boundaries**: Verify that `TableSurface` and `OverlayLayer` do not recompose during standard game cycle loops (deal, hit, stand) when their inputs have not changed.
- **Build**: Ensure the project compiles successfully across all targets after files are moved and visibility modifiers are adjusted.