# Doc's Journal

_Documentation surprises and project-specific KDoc rules discovered during the development of Blackjack._

---

## 2026-04-12 - HapticsService
**Surprise:** The `HapticsService` interface and its implementations (`AndroidHapticsServiceImpl`, `IosHapticsServiceImpl`, `NoOpHapticsService`) were completely undocumented despite being critical for the "premium" user experience.
**Rule:** When documenting services, ensure the interface has the primary documentation and use `@see` or `@inheritDoc` in platform-specific implementations to maintain consistency.

## 2026-04-12 - secureRandom
**Surprise:** `secureRandom` is an undocumented `expect` property that serves as the root source of fairness. While `Random.Default` is fine for UI, a gambling game requires cryptographically secure providers (`java.security.SecureRandom` on JVM/Android, `arc4random` on iOS).
**Rule:** For security-critical `expect` declarations, always document the underlying platform provider in the `commonMain` KDoc.

## 2026-04-12 - GameState.handOutcomes
**Surprise:** `handOutcomes` is a constructor property but was missing from class KDoc. It only holds values in terminal status.
**Rule:** Ensure all constructor parameters in `@Immutable` @Serializable data classes have `@property` tags in class-level KDoc.

## 2026-04-12 - StrategyProvider
**Surprise:** `StrategyProvider` uses `StrategyCell` where `playerValue` is a `String` (e.g., "17+" or "8 or less"). This makes the table compact but requires range-mapping logic.
**Rule:** When documenting strategy tables, mention that `playerValue` labels are descriptive and require range-mapping logic in the UI layer.

## 2026-04-12 - GameOverlay
**Surprise:** `GameOverlay` relies entirely on primitive/provider parameters (`() -> Color`, `() -> Float`) rather than reading `GameState` directly. This avoids recomposing heavy particle systems when unrelated state changes.
**Rule:** When building complex screen overlays, always defer state reads via lambda providers.

## 2026-04-08 - BlackjackAnimationOrchestrator
**Surprise:** The orchestrator manages two distinct asynchronous pipelines (effect-driven and state-driven) that share a `flashJob`. This allows a high-priority "Big Win" effect to cancel a routine "Win" flash.
**Rule:** When orchestrating sensory feedback, separate discrete event handlers from status-based lifecycle reactions.

## 2026-04-07 - GameActionButton
**Surprise:** The `isStrategic` parameter triggers a "breathing" animation (pulsing scale and glow). This draws the player's eye to the mathematically "correct" move.
**Rule:** Look for "breathing" or "pulsing" logic—these often signal high-level game mechanics (like strategic advice).

## 2026-04-05 - HandOutcome
**Surprise:** The `HandOutcome` enum has `NATURAL_WIN` alongside `WIN`, separating a natural blackjack win from a normal score win independently of payout definitions.
**Rule:** Clearly decouple abstract structural statuses from explicit numerical behavior.

## 2026-04-04 - GameActions
**Surprise:** `isCompact` isn't just for mobile portrait; it's triggered whenever more than one hand is active to prevent HUD/card overlap. 
**Rule:** When documenting layout parameters, verify if they are aspect-ratio driven or state-driven.

## 2026-03-30 - SideBetLogic
**Surprise:** `Perfect Pairs` requires an exact match on `Card.rank` (ordinal), not just `Card.rank.value`. A Jack and King do NOT form a "Mixed Pair".
**Rule:** Explicitly state if a "rank match" refers to the literal rank (King == King) or the scoring value (10 == 10).

## 2026-03-29 - BlackjackStateMachine
**Surprise:** The `effects` Flow uses a `channelFlow` wrapper ensuring effects emitted before the first collector arrives are captured.
**Rule:** Note if flows are "hot" (SharedFlow) or "cold-wrapped hot" (channelFlow) and how their lifecycle is bound to the parent.

## 2026-03-29 - Hand
**Surprise:** `isBlackjack` represents a "natural" blackjack (exactly 2 cards totaling 21). Splitting/hitting to 21 does NOT qualify for the 3:2 payout.
**Rule:** Always distinguish between "Natural Blackjack" and "21-score" in both documentation and payout logic.

## 2026-03-29 - GameStatus
**Surprise:** `GameStatus.IDLE` is a default placeholder; the SM initializes with `BETTING`. `GameStatus.PLAYER_WON` is a collective status for the round.
**Rule:** Distinguish between "reachable" application states and "uninitialized" defaults.
