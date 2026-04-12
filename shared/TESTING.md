# Testing in Kotlin Multiplatform (Blackjack)

This project strictly adheres to a testing philosophy optimized for Kotlin Multiplatform (KMP) across Android, iOS, and Desktop (JVM) targets. This guide covers our conventions for coroutines, state machine initialization, and our newly adopted mocking strategy.

---

## 1. Mocks & Fakes Strategy

Historically, KMP projects relied entirely on hand-rolled fakes due to reflection limitations on iOS. We have now adopted **Mokkery** (https://mokkery.dev) as our standard mocking framework for KMP testing.

**Guidelines for Mocking:**
*   **Prefer Mokkery** over older JVM-only libraries (like MockK or Mockito) to ensure tests run across all platforms.
*   **Hand-rolled fakes** (like `FakeDataStore`) remain perfectly valid and are often superior for complex stateful persistence (e.g., `DataStore` or `Room` DAOs) where a simple mock would require extensive stubbing logic.

Example Mokkery usage:
```kotlin
val mockSettings = mock<SettingsRepository> {
    everySuspend { loadHandCount() } returns 2
}
```

---

## 2. Coroutines and State Flows

Testing asynchronous code and `StateFlow` requires precision to avoid flaky delays and race conditions.

*   **`runTest` is Mandatory:** Wrap all suspending tests, flow collections, or `StateFlow` assertions inside `runTest`. Never use real `Thread.sleep` or unbounded `delay()`.
*   **Dispatchers:** Stick to the default `UnconfinedTestDispatcher` provided by `runTest` unless you are explicitly testing advanced concurrent interleaving.
*   **`advanceUntilIdle()`:** When dispatching actions to a coroutine-backed system (like `BlackjackStateMachine`), call `advanceUntilIdle()` before asserting on the resulting state to guarantee all asynchronous updates have completed.

Example:
```kotlin
@Test
fun exampleTest() = runTest {
    // ... setup ...
    sm.dispatch(GameAction.Hit)
    advanceUntilIdle() // Essential: Wait for async state reduction
    assertEquals(GameStatus.PLAYER_WON, sm.state.value.status)
}
```

---

## 3. Turbine for Effects

We use **Turbine** (`app.cash.turbine.test`) exclusively for verifying the emissions of `SharedFlow`s, primarily our `GameEffect` stream.

*   **Never use raw `.first()` or `.take(1)`** on cold or shared flows, as it can lead to missed emissions or hanging tests.

Example:
```kotlin
@Test
fun actionEmitsExpectedEffect() = runTest {
    val sm = testMachine(...)

    sm.effects.test {
        sm.dispatch(GameAction.Hit)
        val emitted = awaitItem() // or buildList for multiple
        assertTrue(GameEffect.PlayCardSound == emitted)
        cancelAndIgnoreRemainingEvents() // Always cancel when done
    }
}
```

---

## 4. State Machine Instantiation

The `BlackjackStateMachine` is the core of our domain logic. You should **never** construct `DefaultBlackjackStateMachine` directly in test classes.

*   **Use `testMachine()`:** This factory (found in `TestFixtures.kt`) auto-wires the state machine to your `TestScope`, ensuring proper cancellation and test dispatcher propagation.

Example:
```kotlin
@Test
fun setupMachine() = runTest {
    // Correct setup:
    val sm = testMachine(
        playingState(
            playerHand = hand(Rank.EIGHT, Rank.THREE),
            dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
            deck = deckOf(Rank.FIVE),
        )
    )
}
```

## 5. Determinism

*   **No Randomness:** Do not use `Random` in tests. Always seed the game with an explicit, predetermined deck sequence via `deckOf(...)`. This guarantees that outcomes (busts, blackjacks, draws) are identical on every test run.
