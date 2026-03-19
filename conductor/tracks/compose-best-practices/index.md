# Track Index - Compose Best Practices & Performance Hardening

Audit and harden `sharedUI` against `COMPOSE_BEST_PRACTICES.md`. Eliminates unnecessary recompositions, improves accessibility semantics, and completes string localization.

## Documentation
- [**Specification**](./spec.md): Full audit results, violations, and requirements.
- [**Implementation Plan**](./plan.md): Step-by-step changes with code snippets.

## Status

**Overall:** ✅ Complete

- **Phase 1: Defer shake animation to draw phase** (`BlackjackScreen.kt`): ✅ Complete
- **Phase 2: Contain pulse animation recomposition scope** (`GameStatusMessage.kt`): ✅ Complete
- **Phase 3: Wrap event lambdas in `remember`** (`GameActions.kt`): ✅ Complete
- **Phase 4: Annotate `GameState` + `Hand` as `@Immutable`** (`GameLogic.kt`): ✅ Complete
- **Phase 5: Add `modifier` parameter to `ActionIcon`**: ✅ Complete (N/A — `ActionIcon` superseded by `ModernActionButton` which already has `modifier` param)
- **Phase 6: Accessibility semantics on `ActionIcon` + balance display**: ✅ Complete (`ControlCenter.FinancialData` Column carries merged semantics; `ModernActionButton` uses Button composable with text label)
- **Phase 7: Localize remaining hardcoded strings**: ✅ Complete

## Prerequisites

None — all changes are self-contained.

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| `@Immutable` annotation over `ImmutableList<>` in domain | Avoids UI-layer dependency in `shared/core`; same Compose compiler result |
| Move `pulseScale` into `GameStatusMessage` | Shrinks recomposition scope from entire screen to status widget only |
| `graphicsLayer` for shake offset | Skips composition + layout phases during per-frame shake animation |
| `remember(audioService, component)` for lambdas | Both are stable lifecycle-scoped references; safe as `remember` keys |
| `semantics(mergeDescendants = true)` on `ActionIcon` | Single accessibility node with meaningful label vs. separate "x 2" + "DOUBLE" announcements |
