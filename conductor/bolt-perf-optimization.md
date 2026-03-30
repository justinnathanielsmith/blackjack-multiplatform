# Bolt Performance Optimization: O(1) Hand Score & Softness Memoization

## Objective
Optimize the `Hand` class by:
1.  **Removing redundant `@Transient` annotations**: Following `kotlinx.serialization` best practices for `by lazy` properties to eliminate compiler warnings and reduce bytecode.
2.  **Consolidating Hand Metrics**: Replacing multiple O(n) passes for `score` and `isSoft` with a single-pass calculation stored in a shared `metrics` lazy property.
3.  **Improving `isSoft` Efficiency**: Deriving softness directly from the remaining unreduced Aces in the score calculation, reducing total card iterations from O(2n) to O(n).

## Background & Motivation
`Hand` is a central data class in `GameState`, read frequently by both the state machine and the UI (especially during dealer draw animations and badge rendering). Currently, `isSoft` and `score` each perform a separate iteration over all cards. Furthermore, the `@Transient` annotation on `by lazy` properties in `@Serializable` classes is redundant and produces compiler warnings, as documented in the latest Bolt journal entry (2026-03-29).

## Scope & Impact
- Target File: `shared/core/src/GameLogic.kt`
- Impact: 
    - Reduces card iterations for combined score/softness checks by 50%.
    - Eliminates redundant bytecode and compiler warnings.
    - O(1) lookup for all derived hand properties after the first read.

## Proposed Solution
Refactor `Hand` to use an internal `HandMetrics` object:

```kotlin
    private data class HandMetrics(val score: Int, val isSoft: Boolean)

    private val metrics: HandMetrics by lazy {
        var s = 0
        var aces = 0
        for (i in 0 until cards.size) {
            val card = cards[i]
            s += card.rank.value
            if (card.rank == Rank.ACE) aces++
        }
        while (s > 21 && aces > 0) {
            s -= 10
            aces--
        }
        HandMetrics(score = s, isSoft = aces > 0)
    }

    val score: Int get() = metrics.score
    val isSoft: Boolean get() = metrics.isSoft
```

## Implementation Steps
1.  Open `shared/core/src/GameLogic.kt`.
2.  Modify the `Hand` data class body:
    -   Define a private `HandMetrics` data class.
    -   Implement the consolidated `metrics` lazy property.
    -   Update `score` and `isSoft` to use `metrics`.
    -   Remove redundant `@Transient` from all body properties (`visibleScore`, `isBust`, etc.).
3.  Clean up the unused `calculateScore` private function.
4.  Update `visibleScore` to use a direct calculation (still O(n), but only used for dealer display).

## Verification & Testing
1.  Run `./amper test -p jvm` to ensure game logic remains correct (especially Ace reduction and soft hands).
2.  Run `./amper build -p jvm` and verify no compiler warnings related to `@Transient`.
3.  Verify that `DealerCard` and `ScoreBadge` correctly display scores and softness animations.
