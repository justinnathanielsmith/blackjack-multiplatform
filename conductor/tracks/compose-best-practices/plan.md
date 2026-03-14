# Implementation Plan - Compose Best Practices & Performance Hardening

All changes target `sharedUI` and `shared/core`. No state machine changes. Phases are independent and can be verified individually.

---

## Phase 1: Defer Shake Animation to Draw Phase (`BlackjackScreen.kt`)

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

## Phase 2: Move `pulseScale` into `GameStatusMessage` (`BlackjackScreen.kt` → `GameStatusMessage.kt`)

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

## Phase 3: Wrap Event Lambdas in `remember` (`GameActions.kt`)

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
    val onStand = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.CLICK)
            component.onAction(GameAction.Stand)
        }
    }
    val onDoubleDown = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.DEAL)
            component.onAction(GameAction.DoubleDown)
        }
    }
    val onSplit = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.DEAL)
            component.onAction(GameAction.Split)
        }
    }
    val onNewGame = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.FLIP)
            component.onAction(GameAction.NewGame())
        }
    }

    AnimatedContent(...) { status ->
        // ... pass remembered lambdas to buttons
        CasinoButton(text = stringResource(Res.string.hit), onClick = onHit, ...)
        CasinoButton(text = stringResource(Res.string.stand), onClick = onStand, ...)
        ActionIcon(icon = "x2", label = stringResource(Res.string.double_down), onClick = onDoubleDown)
        ActionIcon(icon = "⑃", label = stringResource(Res.string.split), onClick = onSplit)
        CasinoButton(text = stringResource(Res.string.new_game), onClick = onNewGame, ...)
    }
}
```

> **Note:** `component` and `audioService` are stable Decompose/service references that do not change during the lifecycle. They are safe `remember` keys.

### Verification
- All game actions (Hit, Stand, Double Down, Split, New Game) still function correctly.
- Lint passes.

---

## Phase 4: Annotate `GameState` as `@Immutable` (`GameLogic.kt`)

### Goal
Enable Compose's strong skipping for composables that receive `GameState` or its fields, by declaring stability at the compiler level.

### Changes

In `shared/core/src/GameLogic.kt`, add `@Immutable` to `GameState` and `Hand`:

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class GameState(
    val playerHands: List<Hand> = listOf(Hand()),
    val playerBets: List<Int> = listOf(0),
    // ... rest unchanged
)

@Immutable
data class Hand(
    val cards: List<Card> = emptyList(),
    // ... rest unchanged
)
```

Check if `Card` also needs `@Immutable` — apply if its fields are all primitive/immutable types.

> **Dependency check:** `androidx.compose.runtime.Immutable` is in `compose-runtime`, which is already a transitive dependency of `sharedUI`. Verify it is available in `shared/core/module.yaml` — if not, add `compose-runtime` as a dependency or use the `kotlinx.collections.immutable` annotation as a fallback.

> **Alternative:** If `compose-runtime` is not appropriate in the domain layer (it introduces a UI dependency), use the `@Stable` annotation from the same package, which has weaker but sufficient semantics for data classes with val-only fields.

### Verification
- `./amper build -m core -p jvm` succeeds.
- `./amper test -m core -p jvm` — all existing tests pass.
- Lint passes.

---

## Phase 5: Add `modifier` Parameter to `ActionIcon` (`ActionIcon.kt`)

### Goal
Follow Compose API convention: `modifier: Modifier = Modifier` must be the first optional parameter.

### Change

```kotlin
@Composable
fun ActionIcon(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,   // ← added
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier              // ← apply here
            .semantics(mergeDescendants = true) { contentDescription = label }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(...)
            .padding(8.dp),
    ) { ... }
}
```

Update all call sites (currently only in `GameActions.kt`) — existing calls without `modifier` still compile due to the default value.

### Verification
- UI renders identically.
- Lint passes (no `ModifierParameter` detekt violations).

---

## Phase 6: Accessibility — Semantics on `ActionIcon` and Header Balance

### Goal
Screen readers announce `ActionIcon` buttons by their label, and the balance display as a single combined description.

### Changes

**`ActionIcon.kt`** (combined with Phase 5):
Replace the root `Column` modifier to include `semantics(mergeDescendants = true)` with `contentDescription = label`:

```kotlin
modifier = modifier
    .semantics(mergeDescendants = true) { contentDescription = label }
    .graphicsLayer { ... }
    .clickable(...)
    .padding(8.dp)
```

This merges "x2" icon text and "DOUBLE" label text into a single "Double" accessibility node.

**`Header.kt`**:
Wrap the balance `Column` with merged semantics:

```kotlin
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Balance: $${animatedBalance.formatWithCommas()}"
    }
) {
    Text("BALANCE", ...)
    Text("$${animatedBalance.formatWithCommas()}", ...)
}
```

### Verification
- Enable TalkBack (Android) or VoiceOver (iOS) and navigate to the game screen.
- "Double Down" button is announced as "Double" (not "x 2").
- Balance row is announced as "Balance: $1,000" (single announcement).

---

## Phase 7: Localize Remaining Hardcoded Strings

### Goal
Replace all hardcoded UI strings with `stringResource` references.

### Step 1 — Add to `sharedUI/composeResources/values/strings.xml`

```xml
<string name="dealer">Dealer</string>
<string name="you">You</string>
<string name="new_game">New Game</string>
<string name="double_down">Double</string>
<string name="split">Split</string>
<string name="balance">Balance</string>
```

### Step 2 — Update source files

**`BlackjackScreen.kt`** (PortraitLayout + LandscapeLayout):
```kotlin
// ❌ Before
HandContainer(title = "Dealer", ...)
HandContainer(title = "You", ...)

// ✅ After
HandContainer(title = stringResource(Res.string.dealer), ...)
HandContainer(title = stringResource(Res.string.you), ...)
```

**`GameActions.kt`** — already handled in Phase 3 (`onNewGame`, `onDoubleDown`, `onSplit` lambdas extract before `AnimatedContent`; label strings update here):
```kotlin
ActionIcon(icon = "x2", label = stringResource(Res.string.double_down), ...)
ActionIcon(icon = "⑃", label = stringResource(Res.string.split), ...)
CasinoButton(text = stringResource(Res.string.new_game), ...)
```

**`Header.kt`**:
```kotlin
// ❌ Before
Text(text = "BALANCE", ...)

// ✅ After
Text(text = stringResource(Res.string.balance), ...)
```

### Step 3 — Rebuild
```bash
./amper build -m sharedUI -p jvm
```

This regenerates the `Res` class. Add explicit imports for each new key:
```kotlin
import sharedui.generated.resources.dealer
import sharedui.generated.resources.you
import sharedui.generated.resources.new_game
import sharedui.generated.resources.double_down
import sharedui.generated.resources.split
import sharedui.generated.resources.balance
```

### Verification
- UI text unchanged in English.
- `./lint.sh` passes.
- No raw string literals remain in UI composables (grep: `Text("` with double-quoted English words).

---

## Verification Plan

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
