# Bolt Performance Optimization: O(1) Hand Score Memoization

## Objective
Optimize the `Hand` class by replacing dynamic, computed properties (which execute O(n) loops over hand cards on every read) with `lazy` delegated properties. This ensures the score and other derived state properties are computed only once per hand instance, providing O(1) lookup during Compose recompositions and game logic transitions.

## Background & Motivation
Currently, `Hand` is part of the `GameState` and is read repeatedly on every render frame and animation loop by the UI (e.g., in `ScoreBadge`, `DealerCard`, `OverlayCardTable`). While `by lazy { ... }` has been added to memoize these properties, they are missing the `@Transient` annotation. Without `@Transient`, `kotlinx.serialization` attempts to serialize the `Lazy` delegate backing field, which can cause runtime crashes (`Serializer for class 'Lazy' is not found`) or unnecessary payload bloat. By adding `@Transient`, we ensure the `lazy` memoization works safely within our serializable state machine.

## Scope & Impact
- Target File: `shared/core/src/GameLogic.kt`
- Impact: O(n) overhead reduced to O(1) for repeated reads of hand properties.

## Proposed Solution
Update `Hand` to use `lazy` delegates and `@Transient` for properties that calculate values based on the cards:
- `score`
- `visibleScore`
- `isBust`
- `isBlackjack`
- `isSoft`

### Example
```kotlin
    @Transient
    val score: Int by lazy { calculateScore(ignoreFaceDown = false) }

    @Transient
    val visibleScore: Int by lazy { calculateScore(ignoreFaceDown = true) }

    @Transient
    val isBust: Boolean by lazy { score > 21 }

    @Transient
    val isBlackjack: Boolean by lazy { cards.size == 2 && score == 21 }

    @Transient
    val isSoft: Boolean by lazy {
        var hasAce = false
        var hardScore = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            if (card.rank == Rank.ACE) {
                hasAce = true
                hardScore += 1
            } else {
                hardScore += card.rank.value
            }
        }
        if (!hasAce) false
        else score != hardScore
    }
```
*Note: Ensure `kotlinx.serialization.Transient` is imported.*

## Implementation Steps
1. Open `shared/core/src/GameLogic.kt`.
2. Import `kotlinx.serialization.Transient`.
3. Modify the `Hand` data class properties (`score`, `visibleScore`, `isBust`, `isBlackjack`, `isSoft`) to use `@Transient` and `by lazy`.

## Verification & Testing
- Run `./amper test -p jvm` to ensure all core tests pass.
- Run `./amper build -p jvm` to verify compilation.
- Ensure that the UI behaves normally when cards are dealt and scores are updated.