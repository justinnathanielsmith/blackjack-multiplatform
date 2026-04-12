## 2026-04-08 - GameFlowMiddleware tests
**Surprise:** Reshuffling logic in `GameFlowMiddleware` intentionally behaves differently in test mode (`isTest = true`) — it only reshuffles when the deck is strictly empty to preserve deterministic sequences set up in `deckOf(...)`, whereas in production it reshuffles at a threshold.
**Rule:** When testing the dealer draw loop in isolation, the `dispatch` mock must manually update the `StateFlow<GameState>` for `GameAction.DealerDraw`, otherwise the middleware will see a stale hand score and potentially loop infinitely or terminate early.
