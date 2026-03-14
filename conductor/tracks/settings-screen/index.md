# Track Index — Settings Screen / Overlay

A settings overlay accessible from any game screen that lets players mute sounds, enable a debug panel, and toggle optional Blackjack rules. All settings are reactive and persisted via DataStore. Built with the Repository pattern and SOLID principles.

## Documentation

- [**Specification**](./spec.md): Functional requirements, architecture, state model changes, and out-of-scope decisions.
- [**Implementation Plan**](./plan.md): Step-by-step development guide with code sketches and test tables.

## Status

- **Step 1: Domain model** (`GameRules`, `BlackjackPayout`, `Surrender` action): ⬜ Not started
- **Step 2: Domain tests** (GameRules + Surrender): ⬜ Not started
- **Step 3: State machine** (`GameRules` integration, `handleSurrender`): ⬜ Not started
- **Step 4: Data layer** (`AppSettings`, `SettingsRepository`, `DataStoreSettingsRepository`): ⬜ Not started
- **Step 5: Data layer tests**: ⬜ Not started
- **Step 6: DI wiring** (`AppGraph` + platform impls): ⬜ Not started
- **Step 7: `DefaultBlackjackComponent`** (settings lifecycle): ⬜ Not started
- **Step 8: `GameEffectHandler`** (mute guard): ⬜ Not started
- **Step 9: UI — `SettingsOverlay`**: ⬜ Not started
- **Step 10: UI — `DebugPanel`**: ⬜ Not started
- **Step 11: UI — `Header` settings icon**: ⬜ Not started
- **Step 12: Wire overlay into screens**: ⬜ Not started
- **Step 13: String resources**: ⬜ Not started
- **Step 14: UI — Surrender button in `GameActions`**: ⬜ Not started
- **Step 15: Lint + final verification**: ⬜ Not started

## Prerequisites

- `advanced-rules` ✅ — `GameAction.Split`, `canDoubleDown()`, and multi-hand state are relied upon by `allowDoubleAfterSplit` and `allowSurrender` rules.
- `betting-system` ✅ — Surrender refund and payout calculations reference `activeBet`.

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| `GameRules` lives in `shared/core`, not `shared/data` | Keeps the state machine dependency-free from persistence; rules are a pure domain value. |
| `SettingsRepository` is an interface (DIP) | Allows in-memory test doubles without DataStore; platform impls are swappable. |
| Single JSON blob in DataStore (not per-key preferences) | `AppSettings` is a cohesive unit; serializing as one string avoids key proliferation and handles nested `GameRules` cleanly. |
| Rule changes apply only on `NewGame` | Mid-hand rule changes would create undefined state; deferring to next game is safe and predictable. |
| Mute enforced in `GameEffectHandler`, not `AudioService` | `AudioService` stays a thin platform wrapper (SRP); mute is an app-level concern, not an audio-driver concern. |
| Debug panel is always-on when enabled (not dismissible) | Debug mode is a developer tool, not a player feature; dismissing it would require re-opening settings. |
| Surrender only on first decision (2-card hand) | Standard casino rule; post-hit surrender adds state machine complexity with minimal gameplay value. |
