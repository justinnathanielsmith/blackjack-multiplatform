# Implementation Plan - Compose Best Practices & Performance Hardening

All changes target `sharedUI` and `shared/core`. No state machine changes. Phases are independent and can be verified individually.

---

## Phase 1: Defer Shake Animation to Draw Phase (`BlackjackScreen.kt`) [x]

### Goal
Remove per-frame recomposition of the entire `BlackjackScreen` tree during the loss shake animation.

### Change
In `BlackjackScreen`, locate the root `BoxWithConstraints` modifier and replace:

```kotlin
// ❌ Before — reads Animatable.value in composition scope
.offset(x = shakeOffset.value.dp)
```

With:

```kotlin
// ✅ After — reads Animatable.value only during draw phase
.graphicsLayer { translationX = shakeOffset.value * density }
```

Remove the `import androidx.compose.foundation.layout.offset` if it is no longer used after this change.

### Verification
- Trigger a loss. The shake animation plays visually unchanged.
- No other layout shifts occur during the shake (content stays centered).
- Lint: `./lint.sh` passes.

---

## Phase 2: Move `pulseScale` into `GameStatusMessage` (`BlackjackScreen.kt` → `GameStatusMessage.kt`) [x]

### Goal
Prevent the entire `BlackjackScreen` from recomposing on every animation frame while the status message pulse runs.

### Changes

**Step 1 — `BlackjackScreen.kt`**: Delete the `rememberInfiniteTransition` / `animateFloat` block (lines ~136–146) and the `pulseScale` variable. Remove `pulseScale` from the `BlackjackGameOverlay` call and its parameter list.

**Step 2 — `BlackjackGameOverlay`**: Remove `pulseScale: Float` from the parameter list. Remove it from the `GameStatusMessage` call.

**Step 3 — `GameStatusMessage.kt`**: Add the `rememberInfiniteTransition` + `animateFloat` call locally:

```kotlin
@Composable
fun GameStatusMessage(status: GameStatus, layoutMode: LayoutMode) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseScale",
    )
    // ... rest of existing composable using pulseScale locally
}
```

### Verification
- Status messages still pulse visually.
- `BlackjackScreen` does NOT recompose every frame when a status message is showing (can verify via Layout Inspector / recomposition counts).
- Lint passes.

---

## Phase 3: Wrap Event Lambdas in `remember` (`GameActions.kt`) [x]

### Goal
Prevent unnecessary recomposition of `CasinoButton` and `ActionIcon` children when `GameActions` recomposes due to state changes.

### Changes

In `GameActions.kt`, extract all inline `onClick` lambdas to `remember`-wrapped vals at the top of the composable:

```kotlin
@Composable
fun GameActions(state: GameState, component: BlackjackComponent, layoutMode: LayoutMode) {
    val audioService = LocalAppGraph.current.audioService

    val onHit = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.DEAL)
            component.onAction(GameAction.Hit)
        }
    }
    // ... items remembered
    
    AnimatedContent(...) { status ->
        // ... pass remembered lambdas to buttons
        CasinoButton(text = stringResource(Res.string.hit), onClick = onHit, ...)
    }
}
```

> **Note:** `component` and `audioService` are stable Decompose/service references that do not change during the lifecycle. They are safe `remember` keys.

### Verification
- All game actions (Hit, Stand, Double Down, Split, New Game) still function correctly.
- Lint passes.

---

## Phase 4: Annotate `GameState` as `@Immutable` (`GameLogic.kt`) [x]

### Goal
Enable Compose's strong skipping for composables that receive `GameState` or its fields, by declaring stability at the compiler level.

### Changes

In `shared/core/src/GameLogic.kt`, add `@Immutable` to `GameState` and `Hand`:

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class GameState(...)
```

> **Note:** This was surpassed by switching to `PersistentList` which provides stronger stability guarantees and enables Strong Skipping more effectively.

### Verification
- `./amper build -m core -p jvm` succeeds.
- `./amper test -m core -p jvm` — all existing tests pass.
- Lint passes.

---

## Phase 5: Add `modifier` Parameter to `ActionIcon` (`ActionIcon.kt`) [x]

### Goal
Follow Compose API convention: `modifier: Modifier = Modifier` must be the first optional parameter.

### Change

```kotlin
@Composable
fun ActionIcon(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) { contentDescription = label }
            ...
    ) { ... }
}
```

### Verification
- UI renders identically.
- Lint passes.

---

## Phase 6: Accessibility — Semantics on `ActionIcon` and Header Balance [x]

### Goal
Screen readers announce `ActionIcon` buttons by their label, and the balance display as a single combined description.

### Changes

**`ActionIcon.kt`**:
Include `semantics(mergeDescendants = true)` with `contentDescription = label`.

**`Header.kt`**:
Wrap the balance `Column` with merged semantics:

```kotlin
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Balance: $${animatedBalance.formatWithCommas()}"
    }
) { ... }
```

### Verification
- TalkBack/VoiceOver test.
- Row/Button description announcements merged correctly.

---

## Phase 7: Localize Remaining Hardcoded Strings [x]

### Goal
Replace all hardcoded UI strings with `stringResource` references.

### Step 1 — Add to `sharedUI/composeResources/values/strings.xml` [x]

```xml
<string name="dealer">Dealer</string>
<string name="you">You</string>
<string name="new_game">New Game</string>
<string name="double_down">Double</string>
<string name="split">Split</string>
<string name="balance">Balance</string>
<string name="status_waiting">Waiting</string>
<string name="result_win">Win</string>
<string name="result_loss">Loss</string>
<string name="result_push">Push</string>
```

### Step 2 — Update source files [x]

**`BlackjackScreen.kt`** — updated `Dealer` and `You` calls.

**`GameActions.kt`** — updated `New Game`, `Double`, `Split`.

**`Header.kt`** — updated `BALANCE`.

**`HandContainer.kt`** — updated `WAITING` and `WIN/LOSS/PUSH`.

### Step 3 — Rebuild [x]
```bash
./amper build -m sharedUI -p jvm
```

### Verification
- UI text unchanged in English.
- `./lint.sh` passes.
- No raw string literals remain in UI composables.

---

## Verification Plan [x]

Run after all phases:

```bash
./amper test -m core -p jvm     # Domain tests — must all pass
./amper build -m sharedUI -p jvm  # UI compiles clean
./lint.sh                         # ktlint + detekt — zero violations
```

Visual checks:
- Loss shake still animates correctly.
- Status message still pulses.
- All game actions respond to taps.
- Balance counter animates on win/loss.
- Confetti fires on win.
