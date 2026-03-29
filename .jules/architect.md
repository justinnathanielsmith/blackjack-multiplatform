# Architect Agent Notes

## Completed Runs

### Run 1 — 2026-03-29
**Violation:** Payout computation (`handNetPayout`, `totalNetPayout`) living in `sharedUI` despite being pure domain logic calling `BlackjackRules.resolveHand()`.
**Location:** `sharedUI/src/ui/components/OverlayCardTable.kt` lines 86–105
**Fix:** Moved `handNetPayout()` and `totalNetPayout()` to `shared/core/src/GameLogic.kt`. These are `GameState` extension functions that perform bet-resolution math (domain responsibility). `handResult()` (maps `HandOutcome` → UI `HandResult` enum) stays in the UI layer since it depends on a UI type.
**PR:** architect/payout-logic-to-domain

## Known Violations (Future Work)
- `LaunchedEffect(isTerminal)` in `BlackjackScreen.kt` contains game flow orchestration (NewGame dispatch with delay) — could be moved to component layer.
- `seatLabel` inline `when` inside composable loop in `BettingPhaseScreen.kt` — minor, could be extracted to a pure function.
