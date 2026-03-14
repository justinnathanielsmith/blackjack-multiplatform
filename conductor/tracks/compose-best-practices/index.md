# Track Index - Compose Best Practices & Performance Hardening

Audit and harden `sharedUI` against `COMPOSE_BEST_PRACTICES.md`. Eliminates unnecessary recompositions, improves accessibility semantics, and completes string localization.

## Documentation
- [**Specification**](./spec.md): Full audit results, violations, and requirements.
- [**Implementation Plan**](./plan.md): Step-by-step changes with code snippets.

## Status

**Overall:** 🟨 In Progress

- **Phase 1: Defer shake animation to draw phase** (`BlackjackScreen.kt`): 🟨 In Progress
- **Phase 2: Contain pulse animation recomposition scope** (`GameStatusMessage.kt`): 🔲 Pending
- **Phase 3: Wrap event lambdas in `remember`** (`GameActions.kt`): 🔲 Pending
- **Phase 4: Annotate `GameState` + `Hand` as `@Immutable`** (`GameLogic.kt`): 🔲 Pending
- **Phase 5: Add `modifier` parameter to `ActionIcon`**: 🔲 Pending
- **Phase 6: Accessibility semantics on `ActionIcon` + Header balance**: 🔲 Pending
- **Phase 7: Localize remaining hardcoded strings**: 🔲 Pending

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
