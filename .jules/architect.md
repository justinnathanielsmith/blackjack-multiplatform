# Architect Agent Notes

## Completed Runs

### Run 1 — 2026-03-29
**Violation:** Payout computation (`handNetPayout`, `totalNetPayout`) living in `sharedUI` despite being pure domain logic calling `BlackjackRules.resolveHand()`.
**Location:** `sharedUI/src/ui/components/OverlayCardTable.kt` lines 86–105
**Fix:** Moved `handNetPayout()` and `totalNetPayout()` to `shared/core/src/GameLogic.kt`. These are `GameState` extension functions that perform bet-resolution math (domain responsibility). `handResult()` (maps `HandOutcome` → UI `HandResult` enum) stays in the UI layer since it depends on a UI type.
**PR:** architect/payout-logic-to-domain

## Known Violations (Future Work)
- None currently explicitly tracked.

## 2026-04-10 — AudioService: two valid consumers, only one is a violation

**Learning:** `audioService` on `BlackjackComponent` serves two consumers:
(1) `BlackjackAnimationOrchestrator` in `BlackjackScreenState` — legitimate presentation wiring, not a Composable calling the service, so NOT a violation.
(2) `BettingPhaseScreen` Composable calling `playEffect()` directly on 6 tap handlers — this IS a violation (UI bypassing the facade).
The test: if the call is inside a `@Composable` function body, it must go through a named `onPlay*()` method. If it's in a coroutine/orchestrator that wires up the effect pipeline, the raw service reference is acceptable.

**Action:** When `audioService` (or any concrete service) appears on a Component interface, check each call site's containing scope — Composable body = violation, coroutine/orchestrator = acceptable. Don't reflexively remove the property from the interface; instead add named action methods for each distinct UI interaction type.
