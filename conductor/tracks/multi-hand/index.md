# Track Index - Multi-Hand Play

Allow the player to choose 1, 2, or 3 independent hands per round. All hands share a uniform bet, are dealt from the same deck, and play sequentially against the same dealer draw. This track also generalizes the existing split model into a unified hand list, replacing the ad-hoc `playerHand`/`splitHand` dual-field approach.

## Documentation
- [**Specification**](./spec.md): Functional requirements, state model refactor, and out-of-scope decisions.
- [**Implementation Plan**](./plan.md): Step-by-step development guide with code sketches and test tables.

## Status

- **Phase 1: Domain Refactor** (GameState, GameAction): ✅ Done
- **Phase 2: State Machine — Deal & NewGame**: ✅ Done
- **Phase 3: State Machine — Hit, Stand, DoubleDown, Split**: ✅ Done
- **Phase 4: Tests**: ✅ Done (65 tests, 14 new multi-hand tests)
- **Phase 5: UI — Hand Count Selector (Betting Phase)**: ✅ Done
- **Phase 6: UI — Multi-Hand Display (Play Phase)**: ✅ Done

## Prerequisites

- `advanced-rules` ✅ — Split and DoubleDown are integrated into the refactored hand list model.
- `ui-juice` ✅ — Per-hand result badges already exist and plug into the new list-based layout.

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| Replace `playerHand + splitHand` with `playerHands: List<Hand>` | Unifies multi-hand and split into one model; removes duplicate routing logic in Hit/Stand. |
| All hands share `currentBet` (uniform bet) | Avoids a per-hand bet selector; keeps betting UX identical to single-hand mode. |
| Extra bet deducted at Deal time, not PlaceBet | PlaceBet UX unchanged; surprise cost at deal time is mitigated by showing total = `bet × count`. |
| Natural BJ check only in single-hand mode | Per-hand immediate BJ resolution in multi-hand adds complexity without proportional value; all hands resolve at dealer turn. |
| Max 4 total hands (initial + splits) | Caps combinatorial complexity; beyond 4 hands the layout degrades without responsive work. |
| `handCount` resets to 1 on NewGame | Stateless default prevents accidental multi-hand replay; player re-selects each round. |
| Split inserts at `activeHandIndex + 1` | Natural left-to-right play order; the split hand is played immediately after the current hand. |
