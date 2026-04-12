# Doc's Journal

## 2026-03-29 - BlackjackStateMachine
**Surprise:** The `effects` Flow uses a `channelFlow` wrapper that manually collects from an internal `MutableSharedFlow` and uses an `isShutdown` state to signal completion. This pattern ensures that any effects emitted between SM initialization and the first collector's arrival are captured (due to `extraBufferCapacity = 64` on the shared flow), and that the flow explicitly completes when the state machine is shut down.
**Rule:** When documenting flows in this architecture, note if they are "hot" (SharedFlow) or "cold-wrapped hot" (channelFlow) and how their lifecycle is bound to the parent component.

## 2026-03-29 - Hand
**Surprise:** `isBlackjack` represents a "natural" blackjack, which must be exactly 2 cards totaling 21. A player reaching 21 via splitting or hitting does NOT qualify for the 3:2 payout, making this boolean distinct from a simple `score == 21` check.
**Rule:** Always distinguish between "Natural Blackjack" and "21-score" in both documentation and payout logic, as they carry different financial implications in the game loop.

## 2026-03-29 - GameStatus
**Surprise:** `GameStatus.IDLE` is defined as the default status for `GameState` but the `BlackjackStateMachine` initializes with `BETTING`. This makes `IDLE` effectively a pre-session placeholder. Also, `GameStatus.PLAYER_WON` is a collective status for the round, triggered if *any* player hand wins.
**Rule:** When documenting state enums, distinguish between "reachable" application states and "uninitialized" defaults like `IDLE`.
## 2026-03-30 - SideBetLogic
**Surprise:** `Perfect Pairs` requires an exact match on `Card.rank` (ordinal rank), not just `Card.rank.value`. A Jack and King, while both 10s, do NOT form a "Mixed Pair". For `21+3`, the "Ace-low straight" is specifically handled (Ace-2-3), which differs from standard numerical sorting of ordinals.
**Rule:** When documenting Blackjack logic, explicitly state if a "rank match" refers to the literal rank (King == King) or the scoring value (10 == 10), as these have distinct definitions in different betting subsystems.

## 2026-04-04 - GameActions
**Surprise:** [isCompact] isn't just for mobile portrait; it's triggered by [BlackjackScreen] whenever more than one hand is active to prevent the HUD from overlapping cards. This aspect of responsive layout is managed via direct observation of [GameState.playerHands].
**Rule:** When documenting layout-related parameters in the HUD, verify if they are aspect-ratio driven (via [LayoutMode]) or state-driven (via [isMultiHand]), as callers need to know which determines the visual density.

## 2026-04-05 - HandOutcome
**Surprise:** The `HandOutcome` enum has `NATURAL_WIN` alongside `WIN`, separating a natural blackjack win from a normal score win independently of payout definitions or wagers. This isolated enum helps avoid conflating payout logic (e.g., 3:2 vs 1:1) from hand resolution logic in `BlackjackRules`.
**Rule:** When documenting game progression boundaries, clearly decouple abstract structural statuses (like `HandOutcome.NATURAL_WIN`) from explicit numerical behavior (like `BlackjackPayout.THREE_TO_TWO`).

## 2026-04-08 - BlackjackAnimationOrchestrator
**Surprise:** The orchestrator manages two distinct asynchronous pipelines (effect-driven and state-driven) that share a `flashJob`. This allows a high-priority "Big Win" effect to cancel a routine "Win" flash. Additionally, it leverages `distinctUntilChangedBy { it.status }` to ensure that animation triggers (like payout eruptions or screen shakes) only fire once per lifecycle transition, preventing duplicates on minor state changes (like balance updates).
**Rule:** When orchestrating sensory feedback, separate discrete event handlers from status-based lifecycle reactions. Explicitly document where jobs are shared across these pipelines to avoid race conditions or animation flickering.

## 2026-04-07 - GameActionButton
**Surprise:** The `isStrategic` parameter doesn't just change colors; it triggers a "breathing" animation (pulsing scale and glow) using an `infiniteRepeatable` transition. This is designed to draw the player's eye to the mathematically "correct" move if one is suggested by the game.
**Rule:** When documenting interactive components, look for "breathing" or "pulsing" logic in the implementation—these often signal high-level game mechanics (like strategic advice) that need to be explained to developers using the component.

## 2026-04-12 - secureRandom
**Surprise:** `secureRandom` is an undocumented `expect` property that serves as the root source of fairness for the player. While the common `Random.Default` is fine for UI, a gambling game requires cryptographically secure providers (`java.security.SecureRandom` on JVM/Android, `arc4random` on iOS) to prevent deck prediction.
**Rule:** For security-critical `expect` declarations, always document the underlying platform provider in the `commonMain` KDoc so future maintainers understand the entropy guarantees. Use `@see` in `actual` blocks to maintain the link without duplication.

## 2026-04-12 - GameState.handOutcomes
**Surprise:** handOutcomes is a constructor property but was missing from class KDoc. It only holds values in terminal status.
**Rule:** Ensure all constructor parameters in @Immutable @Serializable data classes have @property tags in class-level KDoc.
## 2026-04-12 - StrategyProvider
**Surprise:** `StrategyProvider` uses `StrategyCell` where `playerValue` is a `String`. This allows for labels like "17+" or "8 or less" rather than just integers, making the table more compact but requiring logic to map specific hand totals to these ranges.
**Rule:** When documenting strategy tables, mention that `playerValue` labels are descriptive and require range-mapping logic in the UI layer.
