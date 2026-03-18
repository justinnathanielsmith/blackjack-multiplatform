# Implementation Plan - UI Polish & "Juicy" Animations

All changes are confined to `sharedUI`. No domain (`shared/core`) changes are needed. Implement phases in order — each is independent and can be verified individually.

---

## Phase 1: Hole Card Flip Animation (`PlayingCard.kt`)

### Goal
Animate `Card.isFaceDown = false` as a 3D Y-axis flip rather than an instant texture swap.

### Implementation

In `PlayingCard.kt`, replace the static `if (card.isFaceDown)` branch with a transition-driven approach:

```kotlin
val flipTransition = updateTransition(targetState = card.isFaceDown, label = "cardFlip")

val rotationY by flipTransition.animateFloat(
    label = "rotationY",
    transitionSpec = {
        tween(durationMillis = 400, easing = FastOutSlowInEasing)
    }
) { faceDown -> if (faceDown) 0f else 180f }

// Show back face when rotationY < 90, front face when rotationY >= 90
val showBack = rotationY < 90f

Box(
    modifier = Modifier
        .graphicsLayer {
            this.rotationY = rotationY
            cameraDistance = 12f * density
        }
) {
    if (showBack) CardBack(...) else CardFront(...)
}
```

- `cameraDistance` must be set to prevent the card from looking distorted during rotation.
- When `card.isFaceDown` starts as `true` and flips to `false`, the transition runs. When `isFaceDown` is `false` from the start (player cards), `rotationY` is immediately 180° and `CardFront` shows — no animation plays.

### Verification
- Deal a hand: player cards show instantly face-up (no animation artifact).
- Stand: dealer hole card animates with a smooth 3D flip (~400ms).
- New game during dealer turn: no stuck frame.

---

## Phase 2: Card Deal Slide-In Animation (`PlayingCard.kt` / `HandRow.kt`)

### Goal
New cards slide in from their direction of origin when they enter composition.

### Implementation

Add an `offsetY` `Animatable` to `PlayingCard` that starts off-screen and animates to `0`:

```kotlin
@Composable
fun PlayingCard(card: Card, isDealer: Boolean, ...) {
    val offsetY = remember { Animatable(if (isDealer) -300f else 300f) }

    LaunchedEffect(card) {
        delay(animationIndexDelay) // stagger via index param
        offsetY.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 300, easing = DecelerateEasing)
        )
    }

    Box(modifier = Modifier.offset { IntOffset(0, offsetY.value.roundToInt()) }) {
        // existing card content
    }
}
```

Pass an `animationDelay: Int = 0` parameter to `PlayingCard` and populate it from the card's index in `HandRow`:

```kotlin
cards.forEachIndexed { index, card ->
    PlayingCard(card = card, isDealer = isDealer, animationDelay = index * 100)
}
```

> **NOTE:** Key the `LaunchedEffect` on `card` identity (the `Card` data class), not index. This ensures only new cards animate; cards already in the hand don't re-animate on recomposition.

### Verification
- Initial deal: 4 cards slide in sequentially (dealer top, player bottom).
- Hit: new card slides in from player direction.
- Double Down: single new card slides in.
- Split: two new cards animate into their respective hands.

---

## Phase 3: Chip Toss Animation (`BettingPhaseScreen.kt` / `ChipSelector.kt`)

### Goal
A ghost chip animates from the tapped chip button toward the bet display.

### Implementation

Use a `Box` overlay scoped to the betting screen to host the animated ghost chip. Capture the tap position via `Modifier.onGloballyPositioned` and animate a composable using `Animatable` for X, Y, and alpha:

```kotlin
// In BettingPhaseScreen, maintain a list of active chip animations
data class FlyingChip(val startOffset: Offset, val amount: Int, val id: Long)

var flyingChips by remember { mutableStateOf(listOf<FlyingChip>()) }

// In ChipSelector, pass an onChipTapped: (Offset, Int) -> Unit callback
// BettingPhaseScreen launches a coroutine to animate and then remove the chip

LaunchedEffect(flyingChip) {
    // animate x, y to betDisplay position, alpha 1f -> 0f
    flyingChips = flyingChips - flyingChip
}
```

> **Fallback**: If `onGloballyPositioned` hasn't fired yet, skip the animation and update the bet value immediately (the bet state update is never gated on the animation).

### Verification
- Tap $25 chip: ghost chip arcs toward bet display and fades out.
- Multiple rapid taps: each chip animates independently.
- Chips don't appear on the playing screen (scoped to betting phase).

---

## Phase 4: Balance Counter Roll Animation (`Header.kt`)

### Goal
Animate `balance` from its old value to its new value using a counting effect.

### Implementation

```kotlin
@Composable
fun BalanceDisplay(balance: Int) {
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(
            durationMillis = if (abs(balance - previousBalance) > 200) 600 else 300,
            easing = FastOutSlowInEasing
        )
    )
    Text(text = "$${animatedBalance.formatWithCommas()}")
}
```

`animateIntAsState` handles this cleanly without manual `Animatable` management. Extract the formatted balance text into a helper: `Int.formatWithCommas()`.

### Verification
- Win $150: balance rolls from old to new value smoothly.
- Lose $100: balance counts down.
- Place a $25 bet: balance decreases with a short animation.
- New game at $1000: instant reset (no animation needed from arbitrary old state; use `snap()` when resetting from terminal state).

---

## Phase 5: Per-Hand Outcome Badges (`HandContainer.kt` / `BlackjackScreen.kt`)

### Goal
Display WIN/LOSS/PUSH badges on each hand when a split game resolves.

### New Types

Add to `GameLogic.kt`... actually: keep domain pure. Instead, compute `HandResult` in the UI layer from observable `GameState` fields:

```kotlin
// In sharedUI (not shared/core)
enum class HandResult { WIN, LOSS, PUSH, NONE }

fun GameState.primaryHandResult(): HandResult {
    if (!status.isTerminal()) return HandResult.NONE
    val dealerScore = dealerHand.score
    val dealerBust = dealerHand.isBust
    return when {
        playerHand.isBust -> HandResult.LOSS
        dealerBust || playerHand.score > dealerScore -> HandResult.WIN
        playerHand.score == dealerScore -> HandResult.PUSH
        else -> HandResult.LOSS
    }
}

fun GameState.splitHandResult(): HandResult {
    val split = splitHand ?: return HandResult.NONE
    if (!status.isTerminal()) return HandResult.NONE
    // same logic as above but for splitHand + splitBet
}
```

Add a `GameStatus.isTerminal()` extension:
```kotlin
fun GameStatus.isTerminal() = this in setOf(PLAYER_WON, DEALER_WON, PUSH)
```

### Badge Composable

```kotlin
@Composable
fun HandOutcomeBadge(result: HandResult) {
    val color = when (result) {
        HandResult.WIN -> Color(0xFFFFD700)   // gold
        HandResult.LOSS -> Color(0xFFCC2222)  // red
        HandResult.PUSH -> Color(0xFF888888)  // gray
        HandResult.NONE -> return             // render nothing
    }
    val scale by animateFloatAsState(
        targetValue = if (result != HandResult.NONE) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = result.name, color = Color.White, fontWeight = FontWeight.Bold)
    }
}
```

In `HandContainer.kt`, show `HandOutcomeBadge` overlaid on or below each hand row when `splitHand != null`.

### Verification
- Single-hand game: no badges shown; existing `GameStatusMessage` unchanged.
- Split, primary wins / split loses: Hand 1 shows gold WIN, Hand 2 shows red LOSS.
- Both split hands push: both show gray PUSH.

---

## Phase 6: Button Press Scale Feedback (`CasinoButton.kt` / `ActionIcon.kt`)

### Goal
All tap targets scale down on press and spring back.

### Implementation

Add a reusable `pressScale` modifier:

```kotlin
@Composable
fun Modifier.pressScale(scale: Float = 0.93f): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 400f)
    )
    return this
        .scale(animatedScale)
        .clickable(interactionSource = interactionSource, indication = null) { /* no-op */ }
}
```

> **NOTE:** `CasinoButton` and `ActionIcon` already handle their own click logic. Apply `.pressScale()` to the outermost `Box` or `Surface` in each, using their existing `interactionSource` rather than creating a new one. The modifier above is for illustration — integrate with existing interaction handling.

Apply to:
- `CasinoButton.kt` — wrapping `Box`
- `ActionIcon.kt` — wrapping `Box`
- `BetChip.kt` — wrapping `Surface`

### Verification
- Tap Hit button: brief scale-down then spring back.
- Tap chip in betting phase: scale feedback + chip toss animation both play.
- Long press: remains scaled down until release.

---

## Phase 7: Enhanced Win Celebration (`BlackjackScreen.kt`)

### Goal
A more impactful win moment: increased confetti and a brief gold screen flash.

### Implementation

**Confetti**: Locate the existing `ConfettiEffect` call in `BlackjackScreen`. Increase particle count:
```kotlin
// Before:
ConfettiEffect(particleCount = 30)
// After (regular win):
ConfettiEffect(particleCount = 60)
// After (natural Blackjack — detectable via playerHand.cards.size == 2 && playerHand.score == 21 at PLAYER_WON):
ConfettiEffect(particleCount = 120)
```

**Screen flash**: Add a `Box` overlay with animated alpha on top of the game content:
```kotlin
val flashAlpha by animateFloatAsState(
    targetValue = if (status == GameStatus.PLAYER_WON) 0.12f else 0f,
    animationSpec = keyframes {
        durationMillis = 500
        0.12f at 0
        0.12f at 100
        0f at 500
    }
)
Box(modifier = Modifier
    .fillMaxSize()
    .background(Color(0xFFFFD700).copy(alpha = flashAlpha))
    .pointerInput(Unit) {} // absorb touches during flash
)
```

### Verification
- Regular win: confetti bursts (60 particles), brief gold flash.
- Natural Blackjack: larger confetti burst (120 particles), same gold flash.
- Dealer wins / push: no flash, no confetti.

---

## Phase 8: Stand Button Glow (`GameActions.kt`)

### Goal
Provide a visual "heat map" hint by making the "Stand" button glow when the active player's hand score is 19 or 20.

### Implementation

Add a pulsing glow effect to the `ModernActionButton` inside `GameActions.kt` for the "Stand" button.

1. **Calculate the condition:** In `GameActions`, get the current hand score:
```kotlin
val activeHand = state.playerHands.getOrNull(state.activeHandIndex)
val shouldGlowStand = activeHand != null && (activeHand.score == 19 || activeHand.score == 20)
```
2. **Add Glow modifier/properties to `ModernActionButton`:** Update `ModernActionButton` to accept an `isGlowing: Boolean` parameter. If true, apply a repeating shadow or alpha animation using `rememberInfiniteTransition`.
```kotlin
val infiniteTransition = rememberInfiniteTransition(label = "standGlow")
val glowElevation by infiniteTransition.animateFloat(
    initialValue = 4f,
    targetValue = 16f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "glowElevation"
)
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.4f,
    targetValue = 1.0f,
    animationSpec = infiniteRepeatable(
        animation = tween(800, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
    ),
    label = "glowAlpha"
)
// Wrap the Button or use Modifier.graphicsLayer to apply the glow
```
3. **Apply it:** Pass `isGlowing = shouldGlowStand` to the "Stand" `ModernActionButton`.

### Verification
- Stand button normal appearance on 18 or below.
- Stand button visibly pulses/glows when hand score reaches 19 or 20.
- Glow stops if the game state changes to something other than `PLAYING`.

---

## Verification Plan

- **Visual review**: Run on Android emulator and desktop; verify each animation phase independently.
- **Inspection mode**: Add `LocalInspectionMode` guards where needed; preview composables should render statically.
- **Interruption test**: Start a new game mid-dealer-flip; verify no stuck cards or zombie animations.
- **Lint**: `./lint.sh` passes with no violations.
- **No domain tests needed**: All changes are purely presentational; existing state machine tests continue to pass unchanged.

Run existing tests to confirm no regressions: `./amper test -m core -p jvm`
