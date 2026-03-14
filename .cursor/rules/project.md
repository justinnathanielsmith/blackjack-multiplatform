# Project Context

## What This Is
Premium Blackjack game — Kotlin Multiplatform (Android, iOS, Desktop JVM). Standard rules: split, double-down, insurance, dealer AI. State machine + reactive Compose UI.

**Key source files:**
- Domain model & rules: `shared/core/src/GameLogic.kt`
- State machine: `shared/core/src/BlackjackStateMachine.kt`
- Main gameplay screen: `sharedUI/src/ui/screens/BlackjackScreen.kt`
- Betting screen: `sharedUI/src/ui/screens/BettingPhaseScreen.kt`
- String resources: `sharedUI/composeResources/values/strings.xml`

## Build System: JetBrains Amper
No `gradlew`. Use `./amper`.

```bash
./amper build -p jvm                          # Fast: JVM only
./amper test -p jvm                           # Fast: JVM tests only
./amper build -m core -m sharedUI -p jvm      # Specific modules
./amper build                                 # All platforms (slow)
```

Config: `project.yaml` (root), `<module>/module.yaml` (per-module), `gradle/libs.versions.toml` (versions). Dependencies: `$libs.` prefix for version catalog, `$compose.` for Compose Multiplatform.

Flat layout: file path does not need to match package name.

## Module Map
- `shared/core` — Domain: `GameState`, `GameAction`, `BlackjackStateMachine`, `GameLogic`
- `shared/data` — Persistence: Room, DataStore
- `sharedUI` — All Compose UI: components, screens, theme, effects
- `androidApp` / `desktopApp` / `iosApp` — Platform entry points

## Architecture
- **State machine**: `BlackjackStateMachine` holds single `StateFlow<GameState>`; dispatch `GameAction` to mutate
- **Effects**: Side effects (audio, haptics) via `SharedFlow<GameEffect>` — decoupled from state
- **Decompose**: Component lifecycle management
- **Composition Locals**: DI for services via `LocalAppGraph`
- **Immutable domain**: All types `@Serializable`, copy-on-write

### GameState shape
```kotlin
GameState(
    playerHands: List<Hand>,      // multi-hand support built in
    playerBets: List<Int>,        // parallel to playerHands
    activeHandIndex: Int,         // which hand is being played
    handCount: Int,               // 1–3 initial hands
    dealerHand: Hand,
    status: GameStatus,           // BETTING → PLAYING → DEALER_TURN → terminal
    balance: Int,
    currentBet: Int,
    insuranceBet: Int,
)
```

### GameStatus flow
`BETTING → IDLE/PLAYING → (INSURANCE_OFFERED) → DEALER_TURN → PLAYER_WON/DEALER_WON/PUSH`

### GameAction values
`NewGame`, `PlaceBet`, `ResetBet`, `Deal`, `Hit`, `Stand`, `DoubleDown`, `TakeInsurance`, `DeclineInsurance`, `Split`, `SelectHandCount`

## UI Layout Modes
`BlackjackScreen` uses `LayoutMode` enum:
- `PORTRAIT` — `maxHeight > maxWidth`
- `LANDSCAPE_COMPACT` — aspect ratio ≤ 1.8 (phones)
- `LANDSCAPE_WIDE` — aspect ratio > 1.8 (desktop)

## UI Development Protocol
> **Before modifying any `@Composable` screen:**
> 1. Read the corresponding `*Component.kt` interface for the **exact type** emitted by `state: StateFlow<T>`.
> 2. Pass `state` as an **explicit typed parameter** to all extracted private composables — closures do NOT capture the outer `state` variable. Failure causes `Unresolved reference` errors.

## Compose String Resources
- **Never hardcode UI strings.** Use `stringResource(Res.string.xxx)`.
- Add to `sharedUI/composeResources/values/strings.xml`.
- Build project after adding strings (generates `Res` class).
- Add explicit imports: `import sharedui.generated.resources.my_string_key`.

## Testing
Uses `kotlin.test` + `kotlinx.coroutines.test` (`runTest`, `advanceUntilIdle`).
Write tests first. Tracks in `conductor/tracks/<name>/spec.md` and `plan.md`.

## Spec-Driven Development
Tracks in `conductor/tracks/<track-name>/`. Each has `spec.md` (requirements) and `plan.md` (steps).
Always write tests against spec before implementing.
