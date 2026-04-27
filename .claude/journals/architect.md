# Architect Journal

_Non-obvious structural learnings and critical refactoring history for the Blackjack project._

---

## 2026-04-10 — AudioService: two valid consumers, only one is a violation
**Learning:** `audioService` on `BlackjackComponent` serves two consumers:
1. `BlackjackAnimationOrchestrator` in `BlackjackScreenState` — legitimate presentation wiring, not a Composable calling the service, so NOT a violation.
2. `BettingPhaseScreen` Composable calling `playEffect()` directly on 6 tap handlers — this IS a violation (UI bypassing the facade).

**Action:** When `audioService` (or any concrete service) appears on a Component interface, check each call site's containing scope. **Composable body = violation**, whereas a coroutine/orchestrator that wires up the effect pipeline is acceptable. Don't reflexively remove the property; instead add named action methods for each distinct UI interaction type.

## 2026-04-08 - Timing Constants Can Masquerade as Animation Constants
**Learning:** `AutoDealDelayTerminalMs` and `ManualResetDelayMs` were grouped into `AnimationConstants` (ui.theme) because they're millisecond values — visually identical to animation durations. But they drive state machine *behavior* (when to dispatch `GameAction.NewGame`), not visual appearance. The result was an inverted layer dependency: presentation imported from UI.
**Action:** When placing a timing constant, ask whether it controls animation interpolation/duration (ui.theme) or controls when a GameAction is dispatched (presentation layer). If the latter, it belongs in the presentation package.

## 2026-03-29 - BalanceService Interface Extraction Was Zero-Friction
**Learning:** When extracting an interface from a concrete class, naming the interface identically to the old class means zero consumer changes — imports, type annotations, and AppGraph declarations all stay untouched.
**Action:** If the class is already named as a noun/capability (`BalanceService`, `AudioService`) rather than an impl (`BalanceServiceImpl`), reuse the name for the interface to avoid a cascade of consumer edits.

## 2026-03-29 - Payout Calculation Refactor
**Violation:** Payout computation (`handNetPayout`, `totalNetPayout`) living in `sharedUI` despite being pure domain logic calling `BlackjackRules.resolveHand()`.
**Location:** `sharedUI/src/ui/components/OverlayCardTable.kt`
**Fix:** Moved `handNetPayout()` and `totalNetPayout()` to `shared/core/src/GameLogic.kt` as `GameState` extension functions. `handResult()` stays in the UI layer as it maps domain outcomes to UI-specific enums.
**PR:** `architect/payout-logic-to-domain`

---

## 2026-04-25 — Surrender & Insurance: Business Logic Was Hiding in the Reducer
**Violation:** `reduceSurrender`, `reduceTakeInsurance`, `reduceDeclineInsurance`, and `resolveInsuranceOutcome` in `GameReducer.kt` contained business logic: balance calculations, rule guard checks (`rules.allowSurrender`), and dealer-blackjack detection. The Reducer's role is pure state routing — it should dispatch to the domain layer, not implement domain rules.
**Location:** `shared/core/src/state/GameReducer.kt`
**Fix:** Extracted all four functions into `PlayerActionLogic.kt` as public methods. `GameReducer` now delegates to `PlayerActionLogic.surrender/takeInsurance/declineInsurance`. A thin `buildInsuranceResult()` bridge in the Reducer maps the domain `PlayerActionOutcome` back to a `ReducerResult` with the `RunDealerTurn` command when the resolved status is `DEALER_TURN` — keeping infrastructure concerns out of the domain layer.
**Key Signal:** Watch for `ReducerResult` functions that do more than read from state and construct `copy()` — any guard logic or calculation that references domain rules (`rules.*`) is a smell that the logic belongs in `logic/`.
**Tests:** 406/406 pass. `ktlint --format` clean.

---

## 2026-04-27 — Betting Phase: Business Logic Extracted from Reducer
**Violation:** `reducePlaceBet`, `reduceDeal`, and related betting functions in `BettingReducer.kt` contained extensive business logic for enforcing game status, balance checking, and calculating multi-hand wager distributions. This violated the principle that Reducers must remain pure state-routers.
**Location:** `shared/core/src/state/BettingReducer.kt`
**Fix:** Extracted a new `BettingLogic.kt` module containing pure domain functions that return a `BettingActionOutcome`. `BettingReducer.kt` now delegates entirely to these logic functions and merely maps the outcome to a `ReducerResult` with appropriate commands (e.g., `RunDealSequence`).
**Key Signal:** Identical to the previous `PlayerActionLogic` refactor. If a reducer contains `if (amount > state.balance)` or `if (state.status != GameStatus.BETTING)`, it's enforcing rules and needs extraction.
**Tests:** Existing tests (like `BettingEnforcementTest` and `NewGameLogicTest`) pass unchanged, proving the refactor was behaviour-preserving.

---

## Known Violations (Future Work)
- None currently explicitly tracked.
