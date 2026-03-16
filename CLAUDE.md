# Project Instructions

## What This Is
Premium Blackjack game — Kotlin Multiplatform (Android, iOS, Desktop). Standard Blackjack rules: split, double-down, insurance, dealer AI. Uses state machine + reactive UI pattern.

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
./amper test -m core -m sharedUI -p jvm
./amper build                                 # All platforms (slow)
```

Config files: `project.yaml` (root), `<module>/module.yaml` (per-module), `gradle/libs.versions.toml` (versions).

Module layout: `src/` (code), `test/` (tests), `res/` (Android resources), `src@android/` etc. (platform-specific). Flat layout — file path does not need to match package.

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

### GameStatus flow
`BETTING → IDLE/PLAYING → (INSURANCE_OFFERED) → DEALER_TURN → PLAYER_WON/DEALER_WON/PUSH`

## UI Layout Modes
`BlackjackScreen` detects 3 modes via `LayoutMode` enum:
- `PORTRAIT` — `maxHeight > maxWidth`
- `LANDSCAPE_COMPACT` — aspect ratio ≤ 1.8 (phones)
- `LANDSCAPE_WIDE` — aspect ratio > 1.8 (desktop)

## Testing
```bash
./amper test -p jvm
```
Uses `kotlin.test` assertions, `kotlinx.coroutines.test` (`runTest`, `advanceUntilIdle`).
Write tests first (spec-driven). Tracks in `conductor/tracks/<name>/spec.md` and `plan.md`.

## Compose String Resources
- **Never hardcode UI strings.** Use `stringResource(Res.string.xxx)`.
- Add to `sharedUI/composeResources/values/strings.xml`.
- Build project after adding strings (generates `Res` class).
- Add explicit imports: `import sharedui.generated.resources.my_string_key`.

## Linting
```bash
./ktlint --format     # Auto-fix formatting
./lint.sh             # ktlint + detekt (CI check)
jj fix                # Auto-format changed files (jj VCS)
```
Config: `.editorconfig` (ktlint, `package-name` disabled), `config/detekt/detekt.yml` (`InvalidPackageDeclaration` disabled).
**Before committing:** `./ktlint --format` then `./lint.sh`.

## Spec-Driven Development
Tracks live in `conductor/tracks/<track-name>/`. Each track has `spec.md` (requirements) and `plan.md` (steps).
Always write tests against spec before implementing.
