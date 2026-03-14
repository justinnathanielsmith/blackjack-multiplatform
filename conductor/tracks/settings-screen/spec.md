# Specification — Settings Screen / Overlay

## Overview

A settings overlay, accessible from any game screen via a gear icon, lets the player mute sounds, enable a debug panel, and toggle optional Blackjack rules. All settings are reactive (the UI reflects changes instantly) and persisted across sessions via DataStore. The implementation uses the **Repository pattern** and adheres to **SOLID** principles.

---

## Architecture

### Domain layer (`shared/core`)

#### `GameRules` — pure value type, no persistence deps

```kotlin
@Serializable
data class GameRules(
    val dealerHitsSoft17: Boolean = true,
    val allowDoubleAfterSplit: Boolean = true,
    val allowSurrender: Boolean = false,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val deckCount: Int = 6,
)

@Serializable
enum class BlackjackPayout(val numerator: Int, val denominator: Int) {
    THREE_TO_TWO(3, 2),
    SIX_TO_FIVE(6, 5),
}
```

`GameRules` is added as a field on `GameState` so the state machine can reference it at every rule check point without coupling to DataStore.

### Data layer (`shared/data`)

#### `AppSettings`

```kotlin
@Serializable
data class AppSettings(
    val isSoundMuted: Boolean = false,
    val isDebugMode: Boolean = false,
    val gameRules: GameRules = GameRules(),
)
```

#### `SettingsRepository` — interface (DIP)

```kotlin
interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
```

`update` accepts a pure transform lambda so call sites stay testable.

#### `DataStoreSettingsRepository` — concrete implementation (SRP, OCP)

- Persists `AppSettings` as JSON in DataStore `Preferences` under a single `StringPreferencesKey`.
- Serializes/deserializes via `kotlinx.serialization.json.Json`.
- On read error, emits `AppSettings()` (safe default).

### DI (`sharedUI/src/di/AppGraph.kt`)

```kotlin
interface AppGraph {
    // ... existing ...
    val settingsRepository: SettingsRepository
}
```

---

## Features

### FR-SET-1: Settings Entry Point

- A gear icon button (⚙) is always visible in the `Header` composable.
- Tapping it opens the `SettingsOverlay` as a modal.
- Available from both `BettingPhaseScreen` and `BlackjackScreen`.

### FR-SET-2: Sound Mute Toggle

- Toggle switch labeled **"Sound Effects"**.
- When muted, `GameEffectHandler` skips calls to `audioService.playEffect()`.
- The `AudioService` interface does **not** change — mute is enforced at the call site in `GameEffectHandler`.
- Change persists immediately via `settingsRepository.update { it.copy(isSoundMuted = !it.isSoundMuted) }`.

### FR-SET-3: Debug Mode Toggle

- Toggle switch labeled **"Debug Mode"** (shown at the bottom of the settings list).
- When enabled, a `DebugPanel` composable is overlaid on `BlackjackScreen` showing live `GameState` diagnostic info.
- Debug info displayed:
  - `status`, `balance`, `currentBet`, `insuranceBet`
  - `activeHandIndex`, `handCount`, `playerHands.size`
  - `deck.size` (cards remaining)
  - `gameRules` summary

### FR-SET-4: Optional Blackjack Rules

All rule toggles take effect on the **next** `NewGame` (rules mid-hand do not change). Each change persists immediately.

| Setting | UI Label | Default | Description |
| :--- | :--- | :--- | :--- |
| `dealerHitsSoft17` | "Dealer Hits Soft 17" | On | Dealer draws on soft 17 (H17). Off = S17. |
| `allowDoubleAfterSplit` | "Double After Split" | On | Player can double down on a split hand. |
| `allowSurrender` | "Allow Surrender" | Off | Player can surrender (lose half bet) before hitting. |
| `blackjackPayout` | "Blackjack Payout" | 3:2 | Segmented control: 3:2 or 6:5. |
| `deckCount` | "Deck Count" | 6 | Dropdown/segment: 1, 2, 4, 6, 8. |

### FR-SET-5: Reactive Settings

- `SettingsRepository.settingsFlow` is a `Flow<AppSettings>` collected in `DefaultBlackjackComponent`.
- `AppSettings` changes update `AudioService` mute state and `DebugPanel` visibility without requiring a game restart.
- Rule changes do **not** retroactively affect the current hand; they are applied when the next `NewGame` action is dispatched.

### FR-SET-6: GameRules Integration in State Machine

`GameState` gains a `rules: GameRules` field (default `GameRules()`). The state machine references `state.rules` at:

| Rule | Check Location |
| :--- | :--- |
| `dealerHitsSoft17` | Dealer draw loop in `runDealerTurn()` |
| `allowDoubleAfterSplit` | `canDoubleDown()` guard — returns false if on split hand and `!rules.allowDoubleAfterSplit` |
| `allowSurrender` | `handleSurrender()` — action is a no-op unless `rules.allowSurrender` |
| `blackjackPayout` | `resolveOutcome()` — natural BJ payout uses `payout.numerator / payout.denominator` |
| `deckCount` | `buildDeck()` in `handleNewGame()` — constructs `deckCount` standard 52-card decks |

### FR-SET-7: Surrender Action

When `allowSurrender = true`:

- New `GameAction.Surrender` is dispatched.
- Valid only when `status == PLAYING` and `activeHand.cards.size == 2` (first decision only).
- Effect: player loses half their `activeBet`; hand ends; if no more hands, transitions to terminal `DEALER_WON`.
- New `GameEffect.PlayLoseSound` is emitted.
- **Surrender button** shown in `GameActions` composable when valid.

### FR-SET-8: Settings Persistence

- On cold start, `DataStoreSettingsRepository` reads stored settings; `settingsFlow` emits immediately.
- `DefaultBlackjackComponent` awaits the first settings emission before dispatching `NewGame(rules = settings.gameRules)`.
- Settings survive process death (DataStore guarantee).

---

## UI Components

### `SettingsOverlay`

Full-screen or bottom-sheet modal. Sections:

1. **Audio** — Sound Effects mute toggle
2. **Rules** — Dealer Hits Soft 17, Double After Split, Allow Surrender, Blackjack Payout, Deck Count
3. **Developer** — Debug Mode toggle (visually separated at bottom)

Each row: label on the left, control (Switch / SegmentedControl / DropdownMenu) on the right.

A **"Close"** (✕) button dismisses the overlay without requiring a save action (all changes are auto-persisted).

### `DebugPanel`

Semi-transparent overlay anchored to the bottom of `BlackjackScreen`. Visible only when `appSettings.isDebugMode`. Displays a monospaced dump of key `GameState` fields. Tapping outside does not dismiss it (it's always-on when debug mode is active).

### `Header` update

Add a `IconButton` with `Icons.Default.Settings` icon to the existing `Header` composable. The `onSettingsClick: () -> Unit` callback is wired through `BlackjackComponent`.

---

## State Model Changes

### `shared/core/src/GameLogic.kt`

```kotlin
// Add:
data class GameRules(...)
enum class BlackjackPayout(...)

// Modify GameState:
data class GameState(
    ...
    val rules: GameRules = GameRules(),
)

// Modify GameAction:
data class NewGame(
    val initialBalance: Int? = null,
    val rules: GameRules = GameRules(),  // new
) : GameAction()

// Add:
data object Surrender : GameAction()
```

### `shared/data/src/`

New files:
- `AppSettings.kt`
- `SettingsRepository.kt`
- `DataStoreSettingsRepository.kt`

### `sharedUI/src/`

New files:
- `ui/screens/SettingsOverlay.kt`
- `ui/components/DebugPanel.kt`

Modified files:
- `di/AppGraph.kt` — add `settingsRepository`
- `presentation/BlackjackComponent.kt` — add `settingsFlow`, `onSettingsToggle()`
- `ui/components/Header.kt` — add settings icon button
- `ui/effects/GameEffectHandler.kt` — check `isSoundMuted` before `playEffect()`
- `ui/screens/BlackjackScreen.kt` — show `DebugPanel` and settings open state
- `sharedUI/composeResources/values/strings.xml` — add all settings strings

---

## Out of Scope

- **Per-session rule overrides**: Rules only change between games.
- **Rule presets** (e.g., "Vegas Strip", "Atlantic City"): Deferred; single custom config is sufficient.
- **Haptics mute toggle**: `HapticsService` mute is deferred; Vibrate effects are low-priority.
- **Remote config / A/B testing**: Settings are entirely local.
- **Settings import/export**: No file-based backup of preferences.
- **In-game surrender after first hit**: Only first-decision surrender is in scope.
