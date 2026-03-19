# Implementation Plan — Sprint 01: Compose Performance & Lint Stabilization

All work is in `sharedUI` and `shared/core`. No state machine or game logic changes. Epics are ordered by risk: lint first (unblocks CI verification), then perf (highest impact), then hygiene. Each epic is independently verifiable.

---

## Epic 1: Fix Lint Regressions

**Estimated effort:** 1 hour
**Files:** `PlayingCard.kt`, `BlackjackScreen.kt`, `BettingPhaseScreen.kt`
**Risk:** Low — auto-fix handles most violations; one manual fix needed

### Step 1.1 — Auto-fix all formatting violations

```bash
./ktlint --format
```

This resolves the majority of violations across all three files: import ordering, argument wrapping, multiline expression formatting.

### Step 1.2 — Manually fix PlayingCard.kt line-length violation

Locate the line exceeding 120 characters (approximately line 393 post-auto-fix). Break the expression across multiple lines following the existing style in the file. The violation is likely a chained call or multi-argument function — wrap each argument to its own line.

### Step 1.3 — Verify

```bash
./lint.sh
```

Must output 0 violations. If any remain, fix manually before proceeding. Do not move to Epic 2 with lint failures — they will compound.

---

## Epic 2: Fix Compose Recomposition Hotspots

**Estimated effort:** 3 hours
**Files:** `BlackjackScreen.kt`, `BlackjackGameOverlay.kt` (if exists), `GameStatusMessage.kt`, `GameActions.kt`
**Risk:** Medium — animation changes need visual verification; the logic is straightforward

### Step 2.1 — Defer shake animation to draw phase (FR-S01-3)

**File:** `sharedUI/src/ui/screens/BlackjackScreen.kt`

Read the file to find the `.offset(x = shakeOffset.value.dp)` modifier on the root `BoxWithConstraints` or its child. Replace it:

```kotlin
// ❌ Before
.offset(x = shakeOffset.value.dp)

// ✅ After
.graphicsLayer { translationX = shakeOffset.value * density }
```

After replacing, check if `import androidx.compose.foundation.layout.offset` is now unused. If so, remove it to keep lint clean.

**Verify:** Trigger a loss. Shake animates correctly. No layout shift during shake.

### Step 2.2 — Move pulseScale into GameStatusMessage (FR-S01-4)

This is a three-file change.

**File 1 — `BlackjackScreen.kt`:**
- Delete the `rememberInfiniteTransition` block that produces `pulseScale`
- Remove `pulseScale` from the `BlackjackGameOverlay(...)` call

**File 2 — `BlackjackGameOverlay` (wherever it lives — same file or separate):**
- Remove `pulseScale: Float` from the parameter list
- Remove it from the `GameStatusMessage(...)` call

**File 3 — `GameStatusMessage.kt`:**
- Add the `rememberInfiniteTransition` + `animateFloat` locally at the top of the composable:

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
    // ... use pulseScale locally, exactly as before
}
```

> **Note:** The animation spec values must match exactly what was in `BlackjackScreen.kt`. Read the existing code first; do not guess.

**Verify:** Status message pulses visually. `BlackjackScreen` no longer recomposes every frame (can be spot-checked via Layout Inspector if needed).

### Step 2.3 — Wrap GameActions lambdas in remember (FR-S01-5)

**File:** `sharedUI/src/ui/components/GameActions.kt` (or wherever GameActions lives)

Read the file to identify all inline `onClick` lambdas. Extract each to a `remember`-wrapped val before the `AnimatedContent` / return:

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
        { component.onAction(GameAction.Stand) }
    }
    val onDoubleDown = remember(audioService, component) {
        {
            audioService.playEffect(AudioService.SoundEffect.CHIP)
            component.onAction(GameAction.DoubleDown)
        }
    }
    val onSplit = remember(audioService, component) {
        { component.onAction(GameAction.Split) }
    }
    val onNewGame = remember(audioService, component) {
        { component.onAction(GameAction.NewGame) }
    }
    // ... pass remembered lambdas to buttons below
}
```

> **Note:** Read the actual lambdas from the source file — do not assume their contents. The bodies shown above are illustrative. The sound effects, if any, must be preserved exactly.

**Verify:** All game actions (Hit, Stand, Double Down, Split, New Game) respond correctly in manual test.

### Step 2.4 — Lint check

```bash
./lint.sh
```

After Epic 2, lint must still pass. The graphicsLayer import may need to be added; the offset import may need to be removed.

---

## Epic 3: Stability, API Convention & Accessibility

**Estimated effort:** 2 hours
**Files:** `GameLogic.kt`, `ActionIcon.kt`, `Header.kt`
**Risk:** Low — annotation and API surface changes only; no behavioral impact

### Step 3.1 — Annotate GameState and Hand as @Immutable (FR-S01-6)

**File:** `shared/core/src/GameLogic.kt`

Read the file to find the `data class GameState(...)` and `data class Hand(...)` declarations. Add `@Immutable` to both:

```kotlin
import androidx.compose.runtime.Immutable

@Immutable
data class GameState(...)

@Immutable
data class Hand(...)
```

> **Note:** Verify whether `Hand` is already annotated. If `GameState` already has `@Immutable`, this step is a no-op — check first.

**Verify:**
```bash
./amper build -m core -p jvm
./amper test -m core -p jvm
```

Both must succeed. The `@Immutable` annotation is a Compose compiler hint; it does not affect runtime behavior.

### Step 3.2 — Add modifier parameter to ActionIcon (FR-S01-7)

**File:** `sharedUI/src/ui/components/ActionIcon.kt`

Read the file to find the `ActionIcon` composable signature. Add `modifier: Modifier = Modifier` as the first optional parameter (after required params, before `onClick`):

```kotlin
@Composable
fun ActionIcon(
    icon: String,
    label: String,
    modifier: Modifier = Modifier,   // ← add this
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier          // ← apply to root
            .semantics(mergeDescendants = true) { contentDescription = label }
            // ... existing modifiers
    )
}
```

Search all call sites for `ActionIcon(` in the codebase. Verify no call sites pass a positional `modifier` argument that would break. Likely none do since it's a new parameter.

### Step 3.3 — Accessibility semantics on ActionIcon (FR-S01-8)

**File:** `sharedUI/src/ui/components/ActionIcon.kt`

This is done in the same edit as Step 3.2. Add `semantics(mergeDescendants = true) { contentDescription = label }` to the root Column modifier chain. The `label` parameter already exists — wire it to accessibility.

The modifier chain should be ordered: `modifier` (outer) → `semantics` → `graphicsLayer` (animation) → `clickable`. Read the existing modifier chain before editing to preserve the correct order.

### Step 3.4 — Accessibility semantics on Header balance (FR-S01-9)

**File:** `sharedUI/src/ui/components/Header.kt` (or wherever the header balance lives)

Read the file to find the `Column` containing the "BALANCE" label and animated balance value. Wrap it with merged semantics:

```kotlin
Column(
    modifier = Modifier.semantics(mergeDescendants = true) {
        contentDescription = "Balance: $${animatedBalance.formatWithCommas()}"
    }
) {
    // existing BALANCE label and value
}
```

> **Note:** The exact variable name for the animated balance may differ. Read the file first.

### Step 3.5 — Localize 7 hardcoded strings (FR-S01-10)

**Step 3.5a — Add to `sharedUI/composeResources/values/strings.xml`:**

Read the file first to check which strings are already present. Add only the missing ones:

```xml
<string name="dealer">Dealer</string>
<string name="you">You</string>
<string name="new_game">New Game</string>
<string name="double_down">Double</string>
<string name="split">Split</string>
<string name="balance">Balance</string>
```

> Check if `split` and `double_down` already exist — they may have been added during the betting-system or advanced-rules tracks.

**Step 3.5b — Replace hardcoded strings:**

- `BlackjackScreen.kt`: Replace `"Dealer"` → `stringResource(Res.string.dealer)` and `"You"` → `stringResource(Res.string.you)`. These appear in both PortraitLayout and LandscapeLayout branches — replace all occurrences.
- `GameActions.kt`: Replace `"NEW GAME"` / `"New Game"` → `stringResource(Res.string.new_game)`, `"Double"` → `stringResource(Res.string.double_down)`, `"Split"` → `stringResource(Res.string.split)`.
- `Header.kt`: Replace `"BALANCE"` → `stringResource(Res.string.balance)`.

Add explicit imports for each new key:
```kotlin
import sharedui.generated.resources.dealer
import sharedui.generated.resources.you
// etc.
```

**Step 3.5c — Rebuild to regenerate Res class:**
```bash
./amper build -m sharedUI -p jvm
```

---

## Epic 4: Final Verification & Track Close

**Estimated effort:** 30 minutes

### Step 4.1 — Full test suite

```bash
./amper test -p jvm
```

All 188 tests must pass. Any failure here is a regression — investigate before committing.

### Step 4.2 — Full lint pass

```bash
./lint.sh
```

Zero violations required.

### Step 4.3 — Full build

```bash
./amper build -p jvm
```

### Step 4.4 — Visual smoke test

Manually verify (desktop or Android):
- [ ] Loss shake animates correctly (no layout shift)
- [ ] Status message pulses on win/loss/push
- [ ] Hit, Stand, Double Down, Split, New Game all respond
- [ ] Balance counter animates on win/loss
- [ ] All button labels display correctly (no missing strings)
- [ ] UI visually identical to pre-sprint

### Step 4.5 — Update compose-best-practices index

Update `conductor/tracks/compose-best-practices/index.md` to mark all 7 phases ✅ Complete and set overall status to ✅ Complete.

---

## Commit Strategy

One commit per epic is appropriate:

1. `fix: resolve ktlint formatting violations from recent UI commits`
2. `perf: defer animation state reads to draw phase, contain recomposition scopes`
3. `refactor: ActionIcon modifier param, @Immutable stability, accessibility semantics, localize strings`
4. `chore: close compose-best-practices track`

Alternatively, a single squashed commit is acceptable if the engineer prefers:
`perf: complete compose-best-practices track — recomposition, lint, accessibility, i18n`

---

## Risk Register

| Risk | Likelihood | Mitigation |
| :--- | :--- | :--- |
| pulseScale animation spec values differ from assumption | Low | Read actual values from BlackjackScreen.kt before editing |
| ActionIcon call sites break after modifier param add | Very Low | Grep all call sites before editing |
| String keys conflict with existing strings.xml entries | Low | Read strings.xml in full before adding |
| Shake visual regression (layout shifts) | Low | graphicsLayer does not affect layout; verify visually |
| 188 tests fail after @Immutable annotation | Very Low | @Immutable is compiler hint only; no runtime effect |
