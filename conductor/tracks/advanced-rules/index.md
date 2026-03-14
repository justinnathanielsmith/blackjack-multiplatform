# Track Index - Advanced Rules: Double Down, Splitting & Insurance

Add three standard Blackjack player options — Double Down, Insurance, and Splitting — each with independent state machine logic, tests, and UI controls.

## Documentation
- [**Specification**](./spec.md): Functional requirements, state model changes, and out-of-scope decisions for all three features.
- [**Implementation Plan**](./plan.md): Step-by-step development guide with code sketches and test tables.

## Status

- **Phase 1: Domain Models** (GameStatus, GameState, GameAction): ✅ Completed
- **Phase 2: Double Down** (state machine + tests): 📅 Backlog
- **Phase 3: Insurance** (state machine + tests): 📅 Backlog
- **Phase 4: Splitting** (state machine + tests): 📅 Backlog
- **Phase 5: UI — Double Down button**: 📅 Backlog
- **Phase 6: UI — Insurance prompt**: 📅 Backlog
- **Phase 7: UI — Split hand display**: 📅 Backlog

## Prerequisites

- `dealer-hidden-card` ✅ — Insurance depends on `Card.isFaceDown` and `dealerHand.cards[0]` being the face-up card.
- `betting-system` ✅ — All three features interact with `balance` and `currentBet`.

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| Implement in DD → INS → SPL order | Each feature is independent; complexity increases incrementally. |
| `splitHand: Hand?` rather than `List<Hand>` | Avoids a full `playerHand` → `playerHands` refactor; supports one split level, which covers the vast majority of real games. Re-splitting deferred. |
| Mixed split outcomes reported as `PLAYER_WON` | Simplifies terminal status UX; per-hand outcome display is a `ui-juice` enhancement. |
| Insurance on Ace up card only (American rules) | European no-peek rule is out of scope; keeps logic consistent with current hole card implementation. |
| Split Aces receive exactly one card each | Standard casino rule; enforced in `handleHit()` guard. |
