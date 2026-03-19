# Code Review and Fixes for StrategyLogic.kt

## Changes
1. **Fix `@Immutable` Violation:** Update `StrategyCell` to use `ImmutableMap` from `kotlinx.collections.immutable` instead of a standard `Map`. In Jetpack Compose, a standard `Map` is considered unstable, which breaks the `@Immutable` contract and can lead to unnecessary recompositions.
2. **Correct Basic Strategy for A,8:** Update the optimal basic strategy for "A,8" (Soft 19) to correctly DOUBLE against a dealer's 6, rather than STAND.
3. **Refactor and Simplify Logic:** Replace the highly verbose 10-element `mapOf(...)` configurations for Soft and Hard strategies with cleaner, more idiomatic Kotlin using `ALL_UPCARDS.associateWith { ... }` and `when` or `if/else` logic. This drastically reduces lines of code, improves readability, and makes the file less error-prone.

## Verification
1. Run `./amper test -p jvm` to ensure no logic in the state machine or tests relies on the old `Map` interface incorrectly.
2. Launch the app and open the Strategy Guide to verify that it still renders correctly and that "A,8" correctly shows DOUBLE against a dealer 6.