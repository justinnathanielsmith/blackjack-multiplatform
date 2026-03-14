# Implementation Plan - Performance Optimization

## Phase 1: Domain Stability (`GameLogic.kt`)
1. [x] Add `kotlinx.collections.immutable` dependency (if not present).
2. [x] Update `GameState` and `Hand` to use `PersistentList`.
3. [x] Update `GameLogic` functions to use persistent collection APIs (`add`, `set`, etc.).

## Phase 2: UI Optimization (`BlackjackScreen.kt`, `Header.kt`)
1. [x] Wrap background `radialGradient` in `remember`.
2. [x] Refactor `flashAlpha` read into a `drawBehind` modifier on a `Box`.
3. [x] Optimize `Header` to format the balance string once per change.

## Phase 3: Effect Optimization (`ConfettiEffect.kt`)
1. [x] Refactor `ConfettiEffect` burst logic to avoid extra list allocations.
2. [x] Review drawing loop for any other avoidable allocations.

## Verification
1. [x] Run `./amper test -m core -p jvm`
2. [x] Run `./amper build -p jvm`
3. [x] Manual visual check for no regression.
