# 🎨 Compose Best Practices for Blackjack

Lessons learned from MemoryMatch KMP project. These patterns ensure responsive, efficient, and accessible Compose UIs across Android, iOS, and Desktop.

---

## 1. Recomposition & Skipping

### ✅ Strong Skipping with Immutable Collections

**Rule:** Use `PersistentList` and other immutable collections in UI state models to enable Compose's strong skipping.

```kotlin
// ❌ Avoid: Regular List causes skipping issues
data class GameState(
    val playerCards: List<Card>,  // Mutable reference = unstable
)

// ✅ Preferred: Immutable collection enables strong skipping
data class GameState(
    val playerCards: ImmutableList<Card>,  // Stable identity
)
```

**Why:** Compose can only skip recomposition if all parameters are stable. `PersistentList` is structurally stable and persistent, allowing Compose to skip the entire subtree if the list reference hasn't changed.

---

### ✅ Wrap Unstable Lambdas in `remember`

**Rule:** Always wrap event handler lambdas in `remember(dependencies)` when passing to complex sub-components, especially in parents with frequent state updates.

```kotlin
// ❌ Avoid: Creates new lambda on every recomposition
@Composable
fun BettingScreen(onBetPlaced: (Int) -> Unit) {
    Button(onClick = { onBetPlaced(100) }) {  // New lambda every frame
        Text("Bet $100")
    }
}

// ✅ Preferred: Lambda is stable
@Composable
fun BettingScreen(onBetPlaced: (Int) -> Unit) {
    val handleBet = remember { { onBetPlaced(100) } }  // Created once
    Button(onClick = handleBet) {
        Text("Bet $100")
    }
}
```

**Why:** Unstable lambdas force full recomposition of child components even if data is stable. This is critical when a parent recomposes frequently (e.g., due to animations or timers).

---

### ✅ Stabilize Modifiers with `remember`

**Rule:** Wrap complex modifiers (especially those with lambdas like `onGloballyPositioned`) in `remember` to prevent unnecessary child recompositions.

```kotlin
// ❌ Avoid: Creates new Modifier on every composition
@Composable
fun Card(cardId: String) {
    Box(
        modifier = Modifier
            .onGloballyPositioned { position ->
                updateCardPosition(cardId, position)
            }
            .size(100.dp)
    )
}

// ✅ Preferred: Modifier is wrapped in remember
@Composable
fun Card(cardId: String) {
    val modifier = remember(cardId) {
        Modifier
            .onGloballyPositioned { position ->
                updateCardPosition(cardId, position)
            }
            .size(100.dp)
    }
    Box(modifier = modifier)
}
```

**Why:** Unstable modifiers force recomposition of child components on every parent recomposition, even if nothing changed. This defeats optimization efforts.

---

### ✅ Defer State Reads to Draw Phase

**Rule:** For continuous animations that don't affect layout, read `State<T>` in drawing modifiers (`graphicsLayer`, `drawWithCache`) instead of composition scope.

```kotlin
// ❌ Avoid: Recomposition on every animation frame
@Composable
fun AnimatedCard() {
    val alpha by animateFloatAsState(targetValue = 0.5f)
    Box(modifier = Modifier.alpha(alpha))  // Read in composition = recomposition per frame
}

// ✅ Preferred: State read in draw phase only
@Composable
fun AnimatedCard() {
    val alpha = remember { mutableFloatStateOf(0.5f) }
    LaunchedEffect(Unit) {
        // ... animation loop updates alpha.value directly
    }
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha.value })  // Read only during draw
}
```

**Why:** The composition and layout phases are skipped, eliminating recomposition churn on every animation frame. Gravity.

---

### ✅ Provide Keys in Lazy Lists

**Rule:** Always provide stable, unique `key` parameters to `items` in `LazyColumn`, `LazyVerticalGrid`, etc.

```kotlin
// ❌ Avoid: Missing keys cause full recomposition on list changes
LazyColumn {
    items(cards) { card ->
        CardUI(card)
    }
}

// ✅ Preferred: Keys enable efficient item tracking
LazyColumn {
    items(cards, key = { it.id }) { card ->  // Unique key per item
        CardUI(card)
    }
}
```

**Why:** Without keys, Compose recomposes all visible items when the list changes. With keys, only affected items recompose.

---

## 2. Animations & Drawing Performance

### ✅ Cache Expensive Drawing Resources

**Rule:** Use `Modifier.drawWithCache` to create and cache `Path`, `Brush`, and other expensive objects. Reuse them in `onDrawBehind`.

```kotlin
// ❌ Avoid: Allocates new Path every frame
Canvas(modifier = Modifier.size(200.dp)) {
    val path = Path()  // NEW Path object every frame → GC churn
    path.moveTo(0f, 0f)
    path.lineTo(size.width, size.height)
    drawPath(path, color = Color.Black)
}

// ✅ Preferred: Path created once, reused on draw
Box(
    modifier = Modifier
        .size(200.dp)
        .drawWithCache {
            val path = Path()  // Created once when size changes
            path.moveTo(0f, 0f)
            path.lineTo(size.width, size.height)
            onDrawBehind {
                drawPath(path, color = Color.Black)
            }
        }
)
```

**Why:** Reusing drawing resources eliminates allocation churn and GC pauses. Especially important for continuous animations.

---

### ✅ Use `Modifier.layout` for Animated Layout Properties

**Rule:** When animating layout-affecting properties (width, height), use `Modifier.layout` instead of `Modifier.width`/`fillMaxWidth` to avoid recomposition.

```kotlin
// ❌ Avoid: Recomposition on every animation frame
@Composable
fun ProgressBar(progress: Float) {
    val width by animateFloatAsState(targetValue = progress * 200f)
    Box(modifier = Modifier.width(width.dp))  // Layout phase every frame
}

// ✅ Preferred: Animation in layout phase only
@Composable
fun ProgressBar(progress: Float) {
    val widthPx = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(progress) {
        animate(widthPx.value, progress * 200f * density.density) { v, _ ->
            widthPx.value = v
        }
    }
    Box(
        modifier = Modifier.layout { measurable, constraints ->
            val newConstraints = constraints.copy(maxWidth = widthPx.value.toInt())
            val placeable = measurable.measure(newConstraints)
            layout(placeable.width, placeable.height) {
                placeable.placeRelative(0, 0)
            }
        }
    )
}
```

**Why:** Avoids layout phase churn. For continuous animations, prefer reading state during draw phase instead.

---

### ✅ Optimize High-Frequency Animation Loops

**Rule:** For 60fps animation loops, use standard `ArrayList` instead of `mutableStateListOf` when manual frame triggering is already in place.

```kotlin
// ❌ Avoid: State tracking overhead in high-frequency loops
@Composable
fun ParticleEffect() {
    val particles = remember { mutableStateListOf<Particle>() }  // State tracking overhead
    Canvas(modifier = Modifier.fillMaxSize()) {
        withFrameNanos { frameNanos ->
            particles.removeAll { it.isExpired() }  // Triggers state notifications
            particles.forEach { drawParticle(it) }
        }
    }
}

// ✅ Preferred: Standard ArrayList for manual frame loops
@Composable
fun ParticleEffect() {
    val particles = remember { mutableListOf<Particle>() }  // No state tracking
    Canvas(modifier = Modifier.fillMaxSize()) {
        withFrameNanos { frameNanos ->
            for (i in particles.lastIndex downTo 0) {  // Iterate backward for safe removal
                if (particles[i].isExpired()) {
                    particles.removeAt(i)  // In-place removal without allocations
                } else {
                    drawParticle(particles[i])
                }
            }
        }
    }
}
```

**Why:** State tracking and reactive notifications are unnecessary when the frame loop already drives redraws. Backward iteration avoids O(N²) element shifting.

---

### ✅ Avoid Collection Operations in Frame Loops

**Rule:** Don't use `filter`, `map`, or other collection operations in `withFrameNanos`. They allocate new collections per frame.

```kotlin
// ❌ Avoid: New ArrayList allocated per frame
withFrameNanos { _ ->
    val active = particles.filter { !it.isExpired() }  // NEW ArrayList every 60fps frame
    active.forEach { particle -> drawParticle(particle) }
}

// ✅ Preferred: Iterate directly, mutate in-place
withFrameNanos { _ ->
    for (i in particles.lastIndex downTo 0) {
        if (particles[i].isExpired()) {
            particles.removeAt(i)
        }
    }
    for (particle in particles) {
        drawParticle(particle)
    }
}
```

**Why:** Avoids allocation churn and Garbage Collection stalls that cause frame drops on 60fps animation loops.

---

## 3. Composition & Layout Architecture

### ✅ Slot APIs for Flexible Content

**Rule:** Use `@Composable` lambdas (slot APIs) for flexible content composition.

```kotlin
// ❌ Avoid: Rigid structure
@Composable
fun HandCard(card: Card) {
    Box {
        Image(card.suit)
        Text(card.rank)
    }
}

// ✅ Preferred: Flexible slot API
@Composable
fun HandCard(
    card: Card,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {
        Image(card.suit)
        Text(card.rank)
    }
) {
    Box(modifier = modifier) {
        content()
    }
}
```

**Why:** Allows customization without duplicating components. Follows Material Design best practices.

---

### ✅ Modifier First in Parameters

**Rule:** The **first** optional parameter must always be `modifier`.

```kotlin
// ❌ Avoid
@Composable
fun GameButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) { }

// ✅ Preferred
@Composable
fun GameButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) { }
```

**Why:** Enables fluent composition chains and follows Compose conventions.

---

### ✅ Maintain Unidirectional Data Flow (UDF)

**Rule:** Always pass data down and events up. Never pass state setters to children.

```kotlin
// ❌ Avoid: State setter flowing down
@Composable
fun BlackjackScreen(
    gameState: GameState,
    onStateChange: (GameState) -> Unit
) {
    GameUI(gameState, onStateChange)  // Passing setter is not UDF
}

// ✅ Preferred: Events flow up
@Composable
fun BlackjackScreen(gameState: GameState) {
    GameUI(
        gameState = gameState,
        onHit = { dispatchAction(GameAction.Hit) },
        onStand = { dispatchAction(GameAction.Stand) }
    )
}
```

**Why:** UDF simplifies reasoning about state changes and prevents tight coupling.

---

## 4. Accessibility & UX

### ✅ Provide Content Descriptions for Icons

**Rule:** All icon-only buttons must have a `contentDescription`. Never pass `null`.

```kotlin
// ❌ Avoid
IconButton(onClick = { /* restart */ }) {
    Icon(Icons.Default.Refresh, contentDescription = null)  // Invisible to screen readers
}

// ✅ Preferred
IconButton(onClick = { /* restart */ }) {
    Icon(Icons.Default.Refresh, contentDescription = "Restart game")
}
```

**Why:** Screen reader users cannot understand icon-only buttons without descriptions.

---

### ✅ Use Semantic Grouping for Complex Components

**Rule:** When a custom component displays multiple pieces of information, use `Modifier.semantics` on the container.

```kotlin
// ❌ Avoid: Screen readers announce each piece separately
@Composable
fun WalletBadge(balance: Int) {
    Row {
        Icon(Icons.Default.Wallet, contentDescription = "Wallet")
        Text("$balance")
    }
}

// ✅ Preferred: Single, unified description
@Composable
fun WalletBadge(balance: Int) {
    Row(
        modifier = Modifier.semantics { contentDescription = "Wallet: $balance" }
    ) {
        Icon(Icons.Default.Wallet, contentDescription = null)  // Hidden, parent describes
        Text("$balance")
    }
}
```

**Why:** Prevents double announcements and provides clear context to assistive technology.

---

### ✅ Haptic Feedback for Key Interactions

**Rule:** Use `LIGHT` for standard button/selection interactions, `HEAVY` for primary actions (game start, major decisions).

```kotlin
@Composable
fun BetButton(onBet: () -> Unit) {
    val haptics = LocalAppGraph.current.hapticsService
    Button(
        onClick = {
            haptics.perform(HapticFeedback.HEAVY)  // Primary action
            onBet()
        }
    ) {
        Text("Place Bet")
    }
}

@Composable
fun HitButton(onHit: () -> Unit) {
    val haptics = LocalAppGraph.current.hapticsService
    Button(
        onClick = {
            haptics.perform(HapticFeedback.LIGHT)  // Standard interaction
            onHit()
        }
    ) {
        Text("Hit")
    }
}
```

**Why:** Provides distinct physical feedback for different interaction intensities. Improves perceived responsiveness.

---

### ✅ Selection Controls with Proper Roles

**Rule:** Use `Modifier.selectable` with `Role.RadioButton` (or `Role.Tab`) for custom controls, not just `clickable`.

```kotlin
// ❌ Avoid: Screen readers don't know it's selectable
@Composable
fun DifficultySelector(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Easy")
    }
}

// ✅ Preferred: Proper role and semantics
@Composable
fun DifficultySelector(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .size(100.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Easy")
    }
}
```

**Why:** Screen readers announce selection state ("Selected") and proper role ("Radio Button").

---

### ✅ Standardize on `IconButton` for Icon-Only Actions

**Rule:** Use `IconButton` for top bar and action buttons. It enforces the 48dp minimum touch target even for small visual icons.

```kotlin
// ❌ Avoid: Small touch target
@Composable
fun GameTopBar() {
    Row {
        Icon(
            Icons.Default.Close,
            contentDescription = "Back",
            modifier = Modifier.clickable { /* go back */ }
        )
    }
}

// ✅ Preferred: Proper touch target size
@Composable
fun GameTopBar() {
    Row {
        IconButton(onClick = { /* go back */ }) {
            Icon(Icons.Default.Close, contentDescription = "Back")
        }
    }
}
```

**Why:** Ensures accessibility compliance and reduces misclick errors on mobile.

---

## 5. String Resources & Localization

### ✅ Never Hardcode UI Strings

**Rule:** Use `stringResource(Res.string.xxx)` from Compose resources. Never hardcode text.

```kotlin
// ❌ Avoid
@Composable
fun BettingUI() {
    Text("Place Your Bet")  // Not localizable
}

// ✅ Preferred
@Composable
fun BettingUI() {
    Text(stringResource(Res.string.place_your_bet))
}
```

**Why:** Enables localization and centralized text management.

---

### ✅ Add Strings to `composeResources/values/strings.xml`

**Rule:** Update the strings.xml file, then rebuild the project to generate the `Res` class.

```xml
<!-- sharedUI/composeResources/values/strings.xml -->
<string name="place_your_bet">Place Your Bet</string>
<string name="hit">Hit</string>
<string name="stand">Stand</string>
```

```kotlin
// After rebuild, import and use
import sharedui.generated.resources.place_your_bet

@Composable
fun BettingUI() {
    Text(stringResource(Res.string.place_your_bet))
}
```

**Why:** Keeps UI strings organized and enables easy localization across platforms.

---

## 6. Performance Checklist

- [ ] All UI state uses `ImmutableList` or other immutable collections
- [ ] Event handler lambdas are wrapped in `remember`
- [ ] Complex modifiers (with lambdas) are wrapped in `remember`
- [ ] Animation state is read in draw phase (`graphicsLayer`, `drawWithCache`), not composition scope
- [ ] All lazy list items have stable `key` parameters
- [ ] Expensive drawing resources (`Path`, `Brush`) are cached with `drawWithCache`
- [ ] High-frequency animation loops use standard `ArrayList` without state tracking
- [ ] No collection operations (`filter`, `map`) in `withFrameNanos` loops
- [ ] All icon buttons have `contentDescription`
- [ ] Complex components use semantic grouping
- [ ] No hardcoded UI strings (use `stringResource`)
- [ ] Modifier is the first optional parameter in composables

---

## 7. Further Reading

- [Compose Performance — Official Docs](https://developer.android.com/develop/ui/compose/performance)
- [Compose State Stability](https://developer.android.com/develop/ui/compose/performance/stability)
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility)
- MemoryMatch `.Jules/bolt.md` — Detailed performance case studies
- MemoryMatch `.Jules/palette.md` — UX & accessibility patterns
