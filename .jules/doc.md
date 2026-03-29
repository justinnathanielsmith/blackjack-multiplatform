# Doc's Journal

## 2026-03-29 - BlackjackStateMachine
**Surprise:** The `effects` Flow uses a `channelFlow` wrapper that manually collects from an internal `MutableSharedFlow` and uses an `isShutdown` state to signal completion. This pattern ensures that any effects emitted between SM initialization and the first collector's arrival are captured (due to `extraBufferCapacity = 64` on the shared flow), and that the flow explicitly completes when the state machine is shut down.
**Rule:** When documenting flows in this architecture, note if they are "hot" (SharedFlow) or "cold-wrapped hot" (channelFlow) and how their lifecycle is bound to the parent component.

## 2026-03-29 - Hand
**Surprise:** `isBlackjack` represents a "natural" blackjack, which must be exactly 2 cards totaling 21. A player reaching 21 via splitting or hitting does NOT qualify for the 3:2 payout, making this boolean distinct from a simple `score == 21` check.
**Rule:** Always distinguish between "Natural Blackjack" and "21-score" in both documentation and payout logic, as they carry different financial implications in the game loop.

## 2026-03-29 - GameStatus
**Surprise:** `GameStatus.IDLE` is defined as the default status for `GameState` but the `BlackjackStateMachine` initializes with `BETTING`. This makes `IDLE` effectively a pre-session placeholder. Also, `GameStatus.PLAYER_WON` is a collective status for the round, triggered if *any* player hand wins.
**Rule:** When documenting state enums, distinguish between "reachable" application states and "uninitialized" defaults like `IDLE`.
