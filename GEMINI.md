# Project Instructions

## What This Is
Premium Blackjack game — Kotlin Multiplatform (Android, iOS, Desktop JVM). Standard rules: split, double-down, insurance, dealer AI. State machine + reactive Compose UI.

**Key source files:**
- Domain model & rules: `shared/core/src/GameLogic.kt`
- State machine: `shared/core/src/BlackjackStateMachine.kt`
- Main gameplay screen: `sharedUI/src/ui/screens/BlackjackScreen.kt`
- Betting screen: `sharedUI/src/ui/screens/BettingPhaseScreen.kt`
- String resources: `sharedUI/composeResources/values/strings.xml`

## Version Control: Jujutsu (jj)
This project uses **jj** (not git). Use jj commands:
```bash
jj st                     # Status
jj diff                   # Current changes
jj commit -m "Message"    # Finalize commit, start new one
jj log                    # Commit graph
jj edit <revision>        # Modify past commit
jj fix                    # Auto-format changed Kotlin files
```

## Build System: JetBrains Amper
No `gradlew`. Use `./amper`.

```bash
./amper build -p jvm                          # Fast: JVM only
./amper test -p jvm                           # Fast: JVM tests only
./amper build -m core -m sharedUI -p jvm      # Specific modules
./amper test -m core -m sharedUI -p jvm
./amper build                                 # All platforms (slow)
```

Config files: `project.yaml` (root), `<module>/module.yaml` (per-module), `gradle/libs.versions.toml` (versions).

Dependencies: `$libs.` prefix for version catalog, `$compose.` for Compose Multiplatform. No `[bundles]`.

Module layout: `src/` (code), `test/` (tests), `res/` (Android resources), `src@android/` etc. Flat layout — file path does not need to match package.

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

### GameState shape (key fields)
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

### Domain Model Quick Reference
| Type | Key Properties |
|------|----------------|
| `GameState` | see above |
| `Hand` | `cards: List<Card>`, `score: Int`, `visibleScore: Int`, `isBust: Boolean` |
| `Card` | `rank: Rank`, `suit: Suit`, `isFaceDown: Boolean` |
| `GameStatus` | `BETTING`, `IDLE`, `PLAYING`, `INSURANCE_OFFERED`, `DEALER_TURN`, `PLAYER_WON`, `DEALER_WON`, `PUSH` |
| `GameAction` | `NewGame`, `PlaceBet`, `ResetBet`, `Deal`, `Hit`, `Stand`, `DoubleDown`, `TakeInsurance`, `DeclineInsurance`, `Split`, `SelectHandCount` |
| `GameEffect` | `PlayCardSound`, `PlayWinSound`, `PlayLoseSound`, `Vibrate` (sealed) |

### GameStatus flow
`BETTING → IDLE/PLAYING → (INSURANCE_OFFERED) → DEALER_TURN → PLAYER_WON/DEALER_WON/PUSH`


## UI Development Protocol

> **Before modifying any `@Composable` screen:**
> 1. Read the corresponding `*Component.kt` interface to find the **exact type** emitted by `state: StateFlow<T>`.
> 2. Read the domain model in `shared/core/src/` to verify property names and sealed class values.
> 3. Pass `state` as an **explicit typed parameter** to all extracted private composables — Kotlin closures do NOT capture the outer `state` variable. Failure causes `Unresolved reference` errors.

## Compose String Resources
- **Never hardcode UI strings.** Use `stringResource(Res.string.xxx)`.
- Add to `sharedUI/composeResources/values/strings.xml`.
- Build project after adding strings (generates `Res` class).
- Add explicit imports: `import sharedui.generated.resources.my_string_key`.

## Testing
Uses `kotlin.test` assertions, `kotlinx.coroutines.test` (`runTest`, `advanceUntilIdle`).
Write tests first (spec-driven). Tracks in `conductor/tracks/<name>/spec.md` and `plan.md`.

## Design & Animation Reference
This project targets a "premium / juicy" feel. Reference **MemoryMatch** (sibling project) for:
- High-performance particles: `Canvas` + `withFrameNanos` index-based loops
- Screen shakes (`runShakeAnimation`), pulsing glows for game events
- Adaptive mobile/desktop layout helpers
- Curated color palettes (FeltGreen, ModernGold)

When building new UI features, check MemoryMatch for an existing pattern before inventing one.

## Linting
```bash
./ktlint --format     # Auto-fix formatting
./lint.sh             # ktlint + detekt (CI check)
jj fix                # Auto-format changed files
```
Config: `.editorconfig` (ktlint, `package-name` disabled), `config/detekt.yml` (`InvalidPackageDeclaration` disabled).
