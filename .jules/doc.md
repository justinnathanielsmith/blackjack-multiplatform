# Doc's Journal

## 2026-03-29 - BlackjackStateMachine
**Surprise:** The `effects` Flow uses a `channelFlow` wrapper that manually collects from an internal `MutableSharedFlow` and uses an `isShutdown` state to signal completion. This pattern ensures that any effects emitted between SM initialization and the first collector's arrival are captured (due to `extraBufferCapacity = 64` on the shared flow), and that the flow explicitly completes when the state machine is shut down.
**Rule:** When documenting flows in this architecture, note if they are "hot" (SharedFlow) or "cold-wrapped hot" (channelFlow) and how their lifecycle is bound to the parent component.
