# Sprint 01 — Compose Performance & Lint Stabilization

## Sprint Goal

Eliminate all active Compose recomposition hotspots, fix lint regressions introduced by recent UI commits, and fully close the `compose-best-practices` track. No new features. No game logic changes.

---

## Context & Motivation

Recent commits (Player Hand Glow, Premium UI Refactor, Fix performance issues in player hand animations) actively touched animation and layout code. The `compose-best-practices` track was created to audit precisely this area but is 14% complete: Phase 1 is in-progress, Phases 2–7 are pending. The risk of deferring further is that each new UI feature extends recomposition hotspots that are already identified and well-specified.

**Tracks this sprint closes:**
- `compose-best-practices` — 6 phases remaining (Phase 1 in-progress, Phases 2–7 pending)

**Tracks already closed (do not reopen):**
- `performance-optimization` — all phases complete (PersistentList, gradient caching, confetti allocation)
- `code-quality-refinement` — all phases complete (lint.sh, detekt, test coverage)

---

## Problem Areas

### P1 — Full-Screen Recomposition on Every Animation Frame (High Impact)

Two independent root-level animation state reads force the entire `BlackjackScreen` tree to recompose at 60fps even when no game state changes:

**Shake animation** (`BlackjackScreen.kt` ~line 159):
```kotlin
// ❌ Reads Animatable.value in composition → full tree recompose per frame during loss shake
.offset(x = shakeOffset.value.dp)
```

**Pulse animation** (`BlackjackScreen.kt` ~lines 136–146):
```kotlin
// ❌ rememberInfiniteTransition at screen root → full tree recompose every frame while status shows
val pulseScale by infiniteTransition.animateFloat(...)
```

Both animations are visual-only — they do not affect layout measurement. Reading their state in composition scope is a violation of the draw-phase deferral rule.

### P2 — New Lambda Objects on Every GameActions Recomposition (Medium Impact)

All `onClick` lambdas in `GameActions.kt` are constructed inline. When `GameActions` recomposes (e.g., on bet change or hand transition), it creates new lambda instances for all buttons. Downstream `CasinoButton` and `ActionIcon` composables cannot skip because their `onClick` parameter is always a new reference.

### P3 — Lint Regressions From Recent Commits (Medium Impact, CI Blocker)

The `ktlint` format check fails on files modified by recent commits. Violations are present in:
- `sharedUI/src/ui/components/PlayingCard.kt` — import ordering, line length (line ~393), argument wrapping
- `sharedUI/src/ui/screens/BlackjackScreen.kt` — multiline expression wrapping
- `sharedUI/src/ui/screens/BettingPhaseScreen.kt` — argument list wrapping

Most are auto-fixable. One violation in `PlayingCard.kt` (line length) requires manual intervention.

### P4 — Missing Compose Stability Annotation on GameState (Low Impact)

`GameState` and `Hand` are `@Serializable` data classes but not annotated `@Immutable`. Without this annotation, the Compose compiler cannot guarantee stability and may skip strong skipping optimizations even when state is structurally unchanged.

### P5 — Hardcoded UI Strings (Low Impact, Localization Debt)

7 strings bypass `stringResource` in `BlackjackScreen.kt`, `GameActions.kt`, and `Header.kt`. This is not a bug but blocks any future localization and violates the project's resource hygiene rule.

### P6 — Accessibility Gaps on ActionIcon and Header (Low Impact)

- `ActionIcon` renders raw icon symbols without `contentDescription`. Screen readers announce `"x2"` and `"⑃"` rather than `"Double Down"` and `"Split"`.
- Balance display (`"BALANCE"` label + `"$1,000"` value) is announced as two unrelated elements.

---

## Functional Requirements

| ID | Requirement | Priority |
| :-- | :-- | :-- |
| FR-S01-1 | `./lint.sh` must pass with 0 violations after all changes | P0 |
| FR-S01-2 | `./amper test -p jvm` must pass (all 188 tests) | P0 |
| FR-S01-3 | Shake animation must use `.graphicsLayer` draw-phase read | P1 |
| FR-S01-4 | `pulseScale` computation must live inside `GameStatusMessage` | P1 |
| FR-S01-5 | All GameActions `onClick` lambdas must be wrapped in `remember` | P1 |
| FR-S01-6 | `GameState` and `Hand` must be annotated `@Immutable` | P2 |
| FR-S01-7 | `ActionIcon` must have `modifier: Modifier = Modifier` as first optional param | P2 |
| FR-S01-8 | `ActionIcon` root Column must carry `semantics(mergeDescendants=true)` with `contentDescription` | P2 |
| FR-S01-9 | Header balance `Column` must carry merged `semantics` with combined description | P2 |
| FR-S01-10 | 7 hardcoded strings must be moved to `strings.xml` and replaced with `stringResource` | P2 |

---

## Out of Scope

| Item | Reason |
| :--- | :--- |
| `drawWithCache` for PlayingCard checkerboard | Spec explicitly ruled out: draw primitives, not Path/Brush allocations |
| `BlackjackStateMachine` refactor | No game logic changes this sprint |
| Settings Screen feature | Next feature sprint |
| New game rules or side bets | Next feature sprint |
| Any UI visual changes (colors, layout) | Polish sprint is separate track |

---

## Non-Functional Requirements

- No changes to `BlackjackStateMachine` or any `shared/core/src/GameLogic.kt` game rules
- All 188 existing tests must pass unchanged
- No new composable APIs introduced (modifier convention fix to `ActionIcon` is the only API change)
- Visual behavior must be identical to pre-sprint state (shake plays, pulse plays, all actions respond)

---

## Definition of Done

- [ ] `./lint.sh` — 0 violations
- [ ] `./amper test -p jvm` — 188/188 pass
- [ ] `./amper build -p jvm` — clean build
- [ ] `compose-best-practices/index.md` — all phases marked ✅ Complete
- [ ] Visual smoke test: loss shake, status pulse, Hit/Stand/Double/Split/New Game, balance counter
