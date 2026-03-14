# Implementation Plan — Settings Screen / Overlay

Implement in layers, bottom-up: domain model → data layer → DI wiring → state machine integration → UI. Each step is independently testable before the next begins.

---

## Step 1: Domain model — `GameRules` + `Surrender` action (`shared/core`)

### 1a: Add `BlackjackPayout` enum and `GameRules` data class to `GameLogic.kt`

```kotlin
@Serializable
enum class BlackjackPayout(val numerator: Int, val denominator: Int) {
    THREE_TO_TWO(3, 2),
    SIX_TO_FIVE(6, 5),
}

@Serializable
data class GameRules(
    val dealerHitsSoft17: Boolean = true,
    val allowDoubleAfterSplit: Boolean = true,
    val allowSurrender: Boolean = false,
    val blackjackPayout: BlackjackPayout = BlackjackPayout.THREE_TO_TWO,
    val deckCount: Int = 6,
)
```

### 1b: Add `rules` field to `GameState`

```kotlin
data class GameState(
    // ... existing fields ...
    val rules: GameRules = GameRules(),
)
```

All existing serialized states are backward-compatible (default value).

### 1c: Extend `GameAction.NewGame` and add `Surrender`

```kotlin
data class NewGame(
    val initialBalance: Int? = null,
    val rules: GameRules = GameRules(),   // NEW
) : GameAction()

data object Surrender : GameAction()
```

Run: `./amper build -m core -p jvm` — must compile cleanly.

---

## Step 2: Domain tests — `GameRules` defaults and `Surrender` guard (`shared/core/test/`)

Write tests **before** implementing state machine changes.

| Test | Given | Action | Expected |
| :--- | :--- | :--- | :--- |
| `newGame_uses_rules_deckCount` | `NewGame(rules = GameRules(deckCount = 2))` | dispatch | `deck.size == 104` (2 × 52) |
| `newGame_default_deckCount_is_6` | `NewGame()` | dispatch | `deck.size == 312` (6 × 52) |
| `surrender_valid_on_first_decision` | PLAYING, 2-card hand, `rules.allowSurrender = true` | Surrender | `balance += activeBet / 2`, terminal state |
| `surrender_invalid_after_hit` | PLAYING, 3-card hand | Surrender | state unchanged |
| `surrender_no_op_when_rule_disabled` | PLAYING, 2-card hand, `rules.allowSurrender = false` | Surrender | state unchanged |
| `blackjack_payout_3_to_2` | Natural BJ, `rules.blackjackPayout = THREE_TO_TWO` | (after Deal) | `balance += bet * 3 / 2` |
| `blackjack_payout_6_to_5` | Natural BJ, `rules.blackjackPayout = SIX_TO_FIVE` | (after Deal) | `balance += bet * 6 / 5` |
| `dealer_hits_soft17_enabled` | Dealer has soft 17, `dealerHitsSoft17 = true` | (dealer turn) | Dealer draws another card |
| `dealer_stands_soft17_disabled` | Dealer has soft 17, `dealerHitsSoft17 = false` | (dealer turn) | Dealer stops at soft 17 |
| `double_after_split_allowed` | Split active, 2-card hand, `allowDoubleAfterSplit = true` | DoubleDown | normal double down |
| `double_after_split_disallowed` | Split active, 2-card hand, `allowDoubleAfterSplit = false` | DoubleDown | state unchanged |

Run: `./amper test -m core -p jvm` — all new tests should fail (red) at this point.

---

## Step 3: State machine — `GameRules` integration (`BlackjackStateMachine`)

### 3a: Update `handleNewGame()` to use `rules.deckCount`

Replace hardcoded deck construction with:
```kotlin
val deck = (1..rules.deckCount).flatMap { buildDeck() }.shuffled()
```
Store `rules` on the new `GameState`:
```kotlin
_state.value = GameState(balance = ..., rules = action.rules)
```

### 3b: Update natural BJ payout to use `rules.blackjackPayout`

In `handleDeal()` (natural BJ branch):
```kotlin
val payout = (bet * rules.blackjackPayout.numerator) / rules.blackjackPayout.denominator
balance += bet + payout   // return stake + profit
```

### 3c: Update dealer draw loop in `runDealerTurn()` to respect `dealerHitsSoft17`

```kotlin
// Current condition: dealer.score < 17
// New condition:
fun shouldDealerDraw(hand: Hand, rules: GameRules): Boolean {
    if (hand.score < 17) return true
    if (hand.score == 17 && rules.dealerHitsSoft17 && hand.isSoft) return true
    return false
}
```

Add `Hand.isSoft` computed property to `GameLogic.kt`:
```kotlin
val isSoft: Boolean
    get() = cards.any { it.rank == Rank.ACE } &&
        cards.sumOf { it.rank.value } != score   // ace is counting as 11
```

### 3d: Update `canDoubleDown()` to respect `allowDoubleAfterSplit`

```kotlin
fun canDoubleDown(): Boolean =
    activeHand.cards.size == 2 &&
    balance >= activeBet &&
    (activeHandIndex == 0 || rules.allowDoubleAfterSplit)
```

### 3e: Implement `handleSurrender()`

```kotlin
private suspend fun handleSurrender() {
    val state = _state.value
    if (state.status != GameStatus.PLAYING) return
    if (state.activeHand.cards.size != 2) return
    if (!state.rules.allowSurrender) return

    val refund = state.activeBet / 2
    _state.value = state.copy(
        balance = state.balance + refund,
        status = GameStatus.DEALER_WON,
    )
    _effects.emit(GameEffect.PlayLoseSound)
}
```

Wire into `dispatch()`:
```kotlin
is GameAction.Surrender -> handleSurrender()
```

Run: `./amper test -m core -p jvm` — all Step 2 tests should now pass (green).

---

## Step 4: Data layer — `AppSettings` + `SettingsRepository` (`shared/data`)

### 4a: Create `AppSettings.kt`

```kotlin
package io.github.smithjustinn.blackjack.data

import io.github.smithjustinn.blackjack.GameRules
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val isSoundMuted: Boolean = false,
    val isDebugMode: Boolean = false,
    val gameRules: GameRules = GameRules(),
)
```

### 4b: Create `SettingsRepository.kt` (interface — DIP)

```kotlin
package io.github.smithjustinn.blackjack.data

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
}
```

### 4c: Create `DataStoreSettingsRepository.kt`

```kotlin
package io.github.smithjustinn.blackjack.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val SETTINGS_KEY = stringPreferencesKey("app_settings")

class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>
) : SettingsRepository {

    override val settingsFlow: Flow<AppSettings> =
        dataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                prefs[SETTINGS_KEY]?.let {
                    runCatching { Json.decodeFromString<AppSettings>(it) }.getOrNull()
                } ?: AppSettings()
            }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.edit { prefs ->
            val current = prefs[SETTINGS_KEY]
                ?.let { runCatching { Json.decodeFromString<AppSettings>(it) }.getOrNull() }
                ?: AppSettings()
            prefs[SETTINGS_KEY] = Json.encodeToString(transform(current))
        }
    }
}

fun createSettingsRepository(): SettingsRepository =
    DataStoreSettingsRepository(createDataStore())
```

> **NOTE:** `createDataStore()` is the existing `expect fun` in `DataStoreFactory.kt`. Reuse it — do not create a second DataStore instance. The settings key is distinct from the balance key, so they coexist in the same file.

---

## Step 5: Data layer tests (`shared/data/test/`)

Use an in-memory DataStore for tests (no file I/O).

| Test | Action | Expected |
| :--- | :--- | :--- |
| `default_settings_on_first_read` | Read fresh DataStore | `AppSettings()` with all defaults |
| `update_sound_muted` | `update { it.copy(isSoundMuted = true) }` | `settingsFlow` emits `isSoundMuted = true` |
| `update_game_rules` | `update { it.copy(gameRules = GameRules(deckCount = 1)) }` | `settingsFlow` emits `deckCount = 1` |
| `settings_survive_reread` | Write then create new repo instance on same DataStore | Emits same values |
| `corrupt_prefs_returns_default` | Inject malformed JSON string | `AppSettings()` emitted (no crash) |

Run: `./amper test -m data -p jvm`

---

## Step 6: DI wiring — `AppGraph` + platform implementations

### 6a: Add `settingsRepository` to `AppGraph` interface

```kotlin
interface AppGraph {
    // ... existing ...
    val settingsRepository: SettingsRepository
}
```

### 6b: Update each platform `AppGraph` implementation

In `androidApp`, `desktopApp`, `iosApp` — add:
```kotlin
override val settingsRepository: SettingsRepository = createSettingsRepository()
```

> Use the same `DataStore` instance as `BalanceService` if possible to avoid multiple DataStore files, or keep separate files. Simplest: each repository gets its own `createDataStore()` call — DataStore files are differentiated by `DATASTORE_FILE_NAME` constant. Update `DataStoreFactory.kt` to accept a filename parameter, or use separate constants.

---

## Step 7: `DefaultBlackjackComponent` — wire settings into game lifecycle

### 7a: Accept `SettingsRepository` in constructor

```kotlin
class DefaultBlackjackComponent(
    componentContext: ComponentContext,
    private val balanceService: BalanceService,
    private val settingsRepository: SettingsRepository,   // NEW
) : BlackjackComponent, ComponentContext by componentContext {
```

### 7b: Expose settings flow and a settings action

```kotlin
interface BlackjackComponent {
    val state: StateFlow<GameState>
    val effects: SharedFlow<GameEffect>
    val appSettings: StateFlow<AppSettings>   // NEW

    fun onAction(action: GameAction)
    fun onSettingChange(transform: (AppSettings) -> AppSettings)   // NEW
}
```

### 7c: Collect settings in `init`

```kotlin
init {
    componentScope.launch {
        val savedBalance = balanceService.balanceFlow.first()
        val initialSettings = settingsRepository.settingsFlow.first()
        stateMachine.dispatch(
            GameAction.NewGame(
                initialBalance = savedBalance,
                rules = initialSettings.gameRules,
            )
        )
        // ... balance save collection (unchanged) ...
    }
}
```

Convert `settingsFlow` to `StateFlow` for Compose:
```kotlin
override val appSettings: StateFlow<AppSettings> =
    settingsRepository.settingsFlow
        .stateIn(componentScope, SharingStarted.Eagerly, AppSettings())
```

### 7d: Implement `onSettingChange`

```kotlin
override fun onSettingChange(transform: (AppSettings) -> AppSettings) {
    componentScope.launch { settingsRepository.update(transform) }
}
```

---

## Step 8: `GameEffectHandler` — respect mute setting

In `GameEffectHandler.kt`, receive `isSoundMuted: Boolean` as a parameter (recomposed from `appSettings`):

```kotlin
@Composable
fun GameEffectHandler(
    effects: SharedFlow<GameEffect>,
    isSoundMuted: Boolean,        // NEW
    audioService: AudioService,
    hapticsService: HapticsService,
) {
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is GameEffect.PlayCardSound -> {
                    if (!isSoundMuted) audioService.playEffect(SoundEffect.DEAL)
                }
                // ... other effects — all guard on !isSoundMuted ...
                is GameEffect.Vibrate -> hapticsService.vibrate()   // haptics not muted
            }
        }
    }
}
```

Call site in `BlackjackScreen.kt`:
```kotlin
val appSettings by component.appSettings.collectAsState()

GameEffectHandler(
    effects = component.effects,
    isSoundMuted = appSettings.isSoundMuted,
    audioService = appGraph.audioService,
    hapticsService = appGraph.hapticsService,
)
```

---

## Step 9: UI — `SettingsOverlay` composable (`sharedUI/src/ui/screens/SettingsOverlay.kt`)

```kotlin
@Composable
fun SettingsOverlay(
    settings: AppSettings,
    onSettingChange: ((AppSettings) -> AppSettings) -> Unit,
    onDismiss: () -> Unit,
)
```

Layout:
- Full-screen `Dialog` with a `Surface` card (rounded corners, casino green tint).
- `LazyColumn` with section headers and setting rows.
- `SettingToggleRow(label, checked, onToggle)` — reusable row composable.
- `SettingSegmentRow(label, options, selected, onSelect)` — for payout and deck count.

Sections in order:
1. **Audio**: `isSoundMuted` toggle
2. **Rules**: `dealerHitsSoft17`, `allowDoubleAfterSplit`, `allowSurrender`, `blackjackPayout`, `deckCount`
3. **Developer**: `isDebugMode` toggle

> Rule change rows show a small info chip: "Takes effect next game" using `stringResource(Res.string.settings_rule_next_game_note)`.

---

## Step 10: UI — `DebugPanel` composable (`sharedUI/src/ui/components/DebugPanel.kt`)

```kotlin
@Composable
fun DebugPanel(state: GameState)
```

- `Box` anchored to bottom of `BlackjackScreen` with `wrapContentHeight()`.
- Background: `Color.Black.copy(alpha = 0.75f)`.
- Monospaced `Text` (use `FontFamily.Monospace`) displaying:
  ```
  STATUS: PLAYING  BAL: 850  BET: 100
  HAND: 0/1  DECK: 209
  INS: 0  RULES: H17|DAS|6D|3:2
  ```
- Only rendered when `appSettings.isDebugMode`.

---

## Step 11: UI — `Header` settings icon

In `Header.kt`, add an `onSettingsClick: () -> Unit` parameter:

```kotlin
@Composable
fun Header(
    balance: Int,
    onSettingsClick: () -> Unit,   // NEW
)
```

Add `IconButton(onClick = onSettingsClick) { Icon(Icons.Default.Settings, ...) }` to the trailing slot.

Update all `Header` call sites to pass `onSettingsClick`.

---

## Step 12: Wire `SettingsOverlay` into screens

In `BlackjackScreen.kt` and `BettingPhaseScreen.kt`:

```kotlin
var showSettings by remember { mutableStateOf(false) }

Header(
    balance = state.balance,
    onSettingsClick = { showSettings = true },
)

if (showSettings) {
    SettingsOverlay(
        settings = appSettings,
        onSettingChange = component::onSettingChange,
        onDismiss = { showSettings = false },
    )
}
```

---

## Step 13: String resources

Add to `sharedUI/composeResources/values/strings.xml`:

```xml
<!-- Settings -->
<string name="settings_title">Settings</string>
<string name="settings_close">Close</string>
<string name="settings_section_audio">Audio</string>
<string name="settings_section_rules">Rules</string>
<string name="settings_section_developer">Developer</string>
<string name="settings_sound_effects">Sound Effects</string>
<string name="settings_debug_mode">Debug Mode</string>
<string name="settings_dealer_hits_soft17">Dealer Hits Soft 17</string>
<string name="settings_double_after_split">Double After Split</string>
<string name="settings_allow_surrender">Allow Surrender</string>
<string name="settings_blackjack_payout">Blackjack Payout</string>
<string name="settings_deck_count">Deck Count</string>
<string name="settings_payout_3_to_2">3:2</string>
<string name="settings_payout_6_to_5">6:5</string>
<string name="settings_rule_next_game_note">Takes effect next game</string>
<!-- Surrender action -->
<string name="surrender">Surrender</string>
```

Run: `./amper build -m sharedUI -p jvm` to regenerate `Res` class.

---

## Step 14: UI — `GameActions` Surrender button

In `GameActions.kt`, add a **Surrender** button visible when:
- `state.status == GameStatus.PLAYING`
- `state.activeHand.cards.size == 2`
- `state.rules.allowSurrender`

```kotlin
if (state.rules.allowSurrender && state.activeHand.cards.size == 2) {
    CasinoButton(
        text = stringResource(Res.string.surrender),
        onClick = { onAction(GameAction.Surrender) },
    )
}
```

---

## Step 15: Lint and final verification

```bash
./ktlint --format
./lint.sh
./amper test -p jvm
```

### Manual verification checklist

- [ ] Settings icon visible in header on both screens
- [ ] Settings overlay opens and closes cleanly
- [ ] Mute toggle: enable → deal cards → no sound plays
- [ ] Mute toggle: disable → deal cards → sounds play
- [ ] Debug panel: enable → overlay appears with live state values
- [ ] Debug panel: disable → overlay disappears
- [ ] `deckCount = 1`: deck has 52 cards at game start
- [ ] `deckCount = 8`: deck has 416 cards at game start
- [ ] Blackjack payout 6:5: natural BJ pays less than 3:2
- [ ] Dealer S17: dealer stops at soft 17
- [ ] Allow Surrender: button appears; surrender refunds half bet
- [ ] Settings survive app restart (kill + reopen)
- [ ] Rule changes mid-session do not affect current hand
