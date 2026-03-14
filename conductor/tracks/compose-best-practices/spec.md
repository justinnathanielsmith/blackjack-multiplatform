# Specification - Compose Best Practices & Performance Hardening

## Goal

Audit the `sharedUI` module against `COMPOSE_BEST_PRACTICES.md` and resolve all identified violations. This improves recomposition efficiency, reduces GC pressure, improves accessibility, and completes string localization.

---

## Current State Audit

The following items from `COMPOSE_BEST_PRACTICES.md` are **already compliant** and require no changes:

| Rule | Where | Status |
| :--- | :--- | :--- |
| ArrayList + backward iteration in frame loops | `ConfettiEffect.kt` | ✅ |
| Avoid collection ops (`filter`, `map`) in `withFrameNanos` | `ConfettiEffect.kt` | ✅ |
| Stable keys in hand card row | `HandRow.kt` (`key(card)`) | ✅ |
| Lambdas + `MutableInteractionSource` in `remember` | `CasinoButton.kt`, `ActionIcon.kt` | ✅ |
| `graphicsLayer` for press-scale animation (draw phase) | `CasinoButton.kt`, `ActionIcon.kt` | ✅ |
| `animateIntAsState` for balance counter roll | `Header.kt` | ✅ |
| `stringResource` for most action labels | `GameActions.kt`, `BettingPhaseScreen.kt` | ✅ |

The following items are **violations** and are the scope of this track:

---

## Functional Requirements

### FR-BP-1: Defer Shake Animation State Read to Draw Phase

**File:** `BlackjackScreen.kt` — `BlackjackScreen` composable (line ~159).

**Violation:** `shakeOffset.value` is read directly in composition scope:
```kotlin
.offset(x = shakeOffset.value.dp)  // ❌ read in composition = recomposition per frame
```

**Rule:** For continuous animations that don't affect layout, read `State<T>` in draw modifiers (`graphicsLayer`) instead of composition scope.

**Fix:** Replace `.offset(x = ...)` with `.graphicsLayer { translationX = shakeOffset.value * density }`.

- The shake animation is a pure visual effect; it does not affect layout measurement.
- This eliminates per-frame recomposition of the entire `BoxWithConstraints` tree during the loss shake.

---

### FR-BP-2: Contain `pulseScale` Animation Recomposition Scope

**File:** `BlackjackScreen.kt` — `BlackjackScreen` composable (line ~136–146).

**Violation:** `pulseScale` is derived from `rememberInfiniteTransition` at the `BlackjackScreen` root, causing the entire screen to recompose every frame while the status message is pulsing. It is then passed as a `Float` parameter down to `GameStatusMessage`.

**Fix:** Move the `rememberInfiniteTransition` + `animateFloat` call into `GameStatusMessage.kt` (or its direct parent `BlackjackGameOverlay`). Remove the `pulseScale: Float` parameter from `BlackjackGameOverlay` and `GameStatusMessage` signatures; compute it locally instead.

- Recomposition scope for the pulse animation shrinks from the entire screen to `GameStatusMessage` only.
- `BlackjackScreen` no longer recomposes every animation frame.

---

### FR-BP-3: Wrap Event Handler Lambdas in `remember` in `GameActions`

**File:** `GameActions.kt`.

**Violation:** All `onClick` lambdas passed to `CasinoButton` and `ActionIcon` are constructed inline on every recomposition:
```kotlin
// ❌ New lambda object every recomposition
CasinoButton(
    text = stringResource(Res.string.hit),
    onClick = {
        audioService.playEffect(AudioService.SoundEffect.DEAL)
        component.onAction(GameAction.Hit)
    }
)
```

**Fix:** Wrap each event handler in `remember` keyed on stable dependencies (`audioService`, `component`):
```kotlin
val onHit = remember(audioService, component) {
    {
        audioService.playEffect(AudioService.SoundEffect.DEAL)
        component.onAction(GameAction.Hit)
    }
}
CasinoButton(text = stringResource(Res.string.hit), onClick = onHit)
```

Apply to all inline lambdas in `GameActions`: Hit, Stand, Double Down, Split, New Game.

---

### FR-BP-4: Stabilize `GameState` Collections for Strong Skipping

**File:** `shared/core/src/GameLogic.kt`.

**Violation:** `GameState.playerHands: List<Hand>` and `GameState.playerBets: List<Int>` use standard mutable `List`, which Compose cannot prove stable. This prevents strong skipping on composables that receive these fields.

**Fix:** Add `@Immutable` annotation to `GameState` (and `Hand` if not already annotated) to explicitly declare stability to the Compose compiler:
```kotlin
@Immutable
data class GameState(
    val playerHands: List<Hand> = listOf(Hand()),
    val playerBets: List<Int> = listOf(0),
    ...
)
```

> **Note:** `@Immutable` is a contract — the developer promises not to mutate the list after construction. This is already the case given the copy-on-write pattern used throughout the state machine. This is preferred over switching to `ImmutableList<>` to avoid adding the `kotlinx.collections.immutable` dependency to the domain layer.

---

### FR-BP-5: Add `modifier` Parameter to `ActionIcon`

**File:** `ActionIcon.kt`.

**Violation:** `ActionIcon` has no `modifier: Modifier = Modifier` parameter, violating the Compose API convention that modifier is the first optional parameter.

**Fix:**
```kotlin
@Composable
fun ActionIcon(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
)
```

Apply the `modifier` to the root `Column`. Update all call sites to pass `modifier` explicitly if needed.

---

### FR-BP-6: Accessibility — Content Descriptions on `ActionIcon`

**File:** `ActionIcon.kt`, `GameActions.kt`.

**Violation:** `ActionIcon` renders icon text (`"x2"`, `"⑃"`) without a content description. Screen readers announce the raw symbol rather than a meaningful label. The `label` text below the icon is rendered in the UI but not wired to accessibility semantics.

**Fix:** Add `Modifier.semantics { contentDescription = label }` to the root `Column` of `ActionIcon`. The icon `Text` and label `Text` children should be merged under this single description:
```kotlin
Column(
    modifier = modifier
        .semantics(mergeDescendants = true) { contentDescription = label }
        .graphicsLayer { ... }
        .clickable(...),
    ...
)
```

---

### FR-BP-7: Accessibility — Semantic Grouping for Header Balance

**File:** `Header.kt`.

**Violation:** The "BALANCE" label and "$1,000" value are announced as two separate elements by screen readers.

**Fix:** Wrap the balance `Column` in `Modifier.semantics(mergeDescendants = true)` with an explicit combined description:
```kotlin
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Balance: $${animatedBalance.formatWithCommas()}"
    }
) { ... }
```

---

### FR-BP-8: Localize Remaining Hardcoded Strings

**Files:** `BlackjackScreen.kt`, `GameActions.kt`, `Header.kt`.

**Violation:** The following strings are hardcoded and bypass the `stringResource` system:
- `"Dealer"` — `BlackjackScreen.kt` (PortraitLayout + LandscapeLayout)
- `"You"` — `BlackjackScreen.kt` (PortraitLayout + LandscapeLayout)
- `"NEW GAME"` — `GameActions.kt`
- `"Double"` — `GameActions.kt`
- `"Split"` — `GameActions.kt`
- `"BALANCE"` — `Header.kt`

**Fix:**
1. Add to `sharedUI/composeResources/values/strings.xml`:
   ```xml
   <string name="dealer">Dealer</string>
   <string name="you">You</string>
   <string name="new_game">New Game</string>
   <string name="double_down">Double</string>
   <string name="split">Split</string>
   <string name="balance">Balance</string>
   ```
2. Replace each hardcoded string with `stringResource(Res.string.xxx)`.
3. Rebuild to regenerate the `Res` class.

---

## Out of Scope

| Item | Reason |
| :--- | :--- |
| Switching to `ImmutableList<>` in domain | Adds external dependency to `shared/core`; `@Immutable` annotation achieves same Compose compiler result |
| `Modifier.drawWithCache` for card Path objects | Cards use `drawRect`/`drawText` primitives — no expensive Path/Brush allocations to cache |
| `LazyColumn` for hand cards | Card counts are small (2–6); overhead of `LazyColumn` exceeds benefit at this scale |
| Slot API refactor of `CasinoButton` / `ActionIcon` | Current parameterized API is sufficient; slot API would add complexity without clear benefit |

---

## Non-Functional Requirements

- Zero changes to `BlackjackStateMachine` or game logic.
- All existing tests in `shared/core` must pass unchanged: `./amper test -m core -p jvm`.
- Lint must pass: `./lint.sh`.
