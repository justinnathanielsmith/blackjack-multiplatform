# Track Index - UI Polish & "Juicy" Animations

Make the game feel alive. Add motion, timing, and tactile feedback that reward every player action.

## Documentation
- [**Specification**](./spec.md): Functional requirements, animation contracts, and out-of-scope decisions.
- [**Implementation Plan**](./plan.md): Step-by-step development guide with component breakdown and implementation notes.

## Status

- **Phase 1: Hole Card Flip Animation** (dealer reveal): ✅ Completed
- **Phase 2: Card Deal Animation** (slide-in from deck): ✅ Completed
- **Phase 3: Chip Toss Animation** (bet placement feedback): ✅ Completed
- **Phase 4: Balance Counter Animation** (animated number roll): ✅ Completed
- **Phase 5: Per-Hand Outcome Badges** (WIN/LOSS/PUSH on each hand): ✅ Completed
- **Phase 6: Button Press Feedback** (scale bounce on tap): ✅ Completed
- **Phase 7: Enhanced Win Celebration** (full-screen confetti + sound): ✅ Completed

## Prerequisites

- `advanced-rules` ✅ — Split hand display and per-hand outcomes are prerequisites for phase 5.
- `betting-system` ✅ — Balance and chip display needed for phases 3 & 4.

## Key Decisions

| Decision | Rationale |
| :--- | :--- |
| Animate in `sharedUI` only; no state machine changes | All animations are purely presentational; domain state is unchanged. |
| Use `AnimatedContent` / `updateTransition` for state-driven transitions | Compose-native approach; avoids managing coroutine-based animation state in composables. |
| `PlayingCard` flip driven by `Card.isFaceDown` state change | Existing `isFaceDown` field already carries the trigger; composable observes it and plays transition. |
| Card deal slide-in keyed by card identity | Each card animates once when it enters composition, using `LaunchedEffect` + `Animatable`. |
| Per-hand outcome badges deferred from `advanced-rules` | Mixed outcomes (one win, one loss on split) need visible per-hand labels; terminal `GameStatus` alone is insufficient. |
| Balance counter roll animation | A counting animation reinforces win/loss magnitude without extra state. |
