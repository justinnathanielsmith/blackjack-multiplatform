# Phase 6: Performance Audit

## Objective
Perform an evidence-driven pass using Compose compiler metrics and Layout Inspector to catch what structural refactoring may have missed. Ensure Phase 5 sub-composables are skippable, remove unstable parameter types, and eliminate unnecessary recompositions.

## Background & Motivation
Phase 5 decomposed the `BlackjackScreen` into smaller sub-composables (`TableSurface`, `GameOverlay`, `BettingLayer`, `OverlayLayer`). However, some of these may still take unstable parameters (like `GameState` instead of specific fields, or data classes without `@Immutable`), or read `Animatable.value` or `derivedStateOf` at the wrong scope, leading to unnecessary recompositions.

## Scope & Impact
- **Target Files**: 
  - `sharedUI/src/ui/screens/BettingLayer.kt`
  - `sharedUI/src/ui/screens/BettingPhaseScreen.kt`
  - `sharedUI/src/ui/screens/GameOverlay.kt`
  - `sharedUI/src/ui/screens/SplashScreen.kt`
  - `sharedUI/src/ui/components/PayoutAnimations.kt`
  - `sharedUI/src/ui/animation/BlackjackAnimationState.kt`
  - `shared/data/src/AppSettings.kt`
- **Impact**: 
  - Sub-composables will be marked skippable. 
  - `TableSurface` and `OverlayLayer` will have 0 recompositions during gameplay. 
  - Eliminates O(Frames) recompositions during animations.

## Implementation Steps

### 1. Replace `GameState` parameter with specific fields
- **`sharedUI/src/ui/screens/BettingLayer.kt`**: Replace `state: GameState` with specific fields: `status: GameStatus`, `handCount: Int`, `sideBets: Map<SideBetType, Int>`, and `playerHands: List<Hand>`.
- **`sharedUI/src/ui/screens/BettingPhaseScreen.kt`**: Replace `state: GameState` with the specific fields it actually accesses: `handCount: Int`, `sideBets: Map<SideBetType, Int>`, and `playerHands: List<Hand>`. Update `BlackjackScreen` and `BettingLayer` to pass these extracted properties.

### 2. Move `derivedStateOf` into sub-composables
- **`sharedUI/src/ui/screens/BlackjackScreen.kt`**: Remove the `showStatus` and `isBlackjack` derived states, and stop passing them to `GameOverlay`.
- **`sharedUI/src/ui/screens/GameOverlay.kt`**: Add `showStatus` and `isBlackjack` locally within `BlackjackGameOverlay` (or `GameOverlay`) using `remember(status, playerHands) { derivedStateOf { ... } }`. This prevents the parent `BlackjackScreen` from recomposing when these derived values change.

### 3. Demote `Animatable.value` reads to draw/layer scope
- **`sharedUI/src/ui/screens/SplashScreen.kt`**: 
  - Replace `.scale(scale.value).alpha(alpha.value)` with `.graphicsLayer { scaleX = scale.value; scaleY = scale.value; alpha = alpha.value }`.
- **`sharedUI/src/ui/components/PayoutAnimations.kt`**: 
  - Replace `.offset(y = offsetY.value.dp).alpha(alphaAnim.value)` with `.graphicsLayer { translationY = offsetY.value.dp.toPx(); alpha = alphaAnim.value }`.

### 4. Annotate data classes with `@Immutable`
- **`sharedUI/src/ui/animation/BlackjackAnimationState.kt`**: Add the `@Immutable` annotation to `PayoutInstance`, `ChipEruptionInstance`, and `ChipLossInstance`.
- **`shared/data/src/AppSettings.kt`**: Add the `@Immutable` annotation to `AppSettings` to ensure `OverlayLayer` treats it as a stable parameter.

## Verification & Testing
1. Enable Compose compiler metrics in `sharedUI/module.yaml` or `build.gradle.kts`:
   ```kotlin
   freeCompilerArgs += [
       "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=<path>",
       "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=<path>"
   ]
   ```
2. Generate reports and verify that all Phase 5 sub-composables (`TableSurface`, `GameOverlay`, `BettingLayer`, `OverlayLayer`) are marked as `skippable` and have no unstable parameters.
3. Run Layout Inspector and verify:
   - `TableSurface`: 0 recompositions during a full game cycle.
   - `OverlayLayer`: 0 recompositions when all overlays are closed.
4. Run `./amper test -p jvm` to ensure all existing tests pass.