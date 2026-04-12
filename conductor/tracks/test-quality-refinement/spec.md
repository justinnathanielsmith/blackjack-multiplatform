# Specification: Test Quality Refinement

## Goal
Establish a robust, DRY testing DSL that eliminates boilerplate and ensures the test suite adheres strictly to SOLID principles, specifically improving Single Responsibility and Open/Closed properties.

## Requirements

### 1. Eliminate DRY Violations in State Instantiation
- **`TestGameStateBuilder` or Fixtures**: The current `playingState()` function often repeats heavy defaults.
  - Create a builder or dedicated test-fixture classes in a `fixtures` package for constructing `GameState`.
  - Use `copy()`-like operations strictly from shared baselines, making tests independent of the explicit `GameState` constructor parameters (OCP).
  - e.g., `GameStateFixtures.playing(bet = 100) { withDealerHand(Rank.TEN, Rank.NINE) }`

### 2. Eliminate DRY Violations in State Machine Observation
- **Turbine Boilerplate Reduction**: Exerting a `GameAction` inside `sm.state.test` requires ~5 lines of boilerplate.
  - Create extension functions to fold the `awaitItem`, `dispatch`, and `cancel` semantics.
  - Example target API: `sm.assertTransition(action = GameAction.Deal) { state -> assertEquals(...) }`

### 3. Resolve SOLID Violations (Single Responsibility Principle)
- **Separation of Concerns in Logic Tests**: Logic objects like `PlayerActionLogic` return combined outcomes (State + Side Effects).
  - Extract test cases so that state-transition rules and side-effect rules are tested in *separate* test methods.
  - E.g., `hit_advancesTurn_whenHandBusts` and `hit_addsBustEffect_whenHandBusts` should not randomly intermix assertions over the entire returned bundle if it obfuscates the domain logic.

### 4. Condense Redundant Scenarios via Parameterized Testing
- **Input/Output Mapping**: `BettingPhaseTest` contains multiple test cases doing the same structural assertions but verifying boundary conditions.
  - Use `kotlin.test` collections (e.g., `listOf(...).forEach { (input, expected) -> ... }`) to build parameterized suites for rejection mapping (like invalid bet amounts, out-of-phase actions, invalid state commands).

## Success Criteria
- [ ] Boilerplate `sm.state.test` blocks are replaced with a single reusable extension wrapper.
- [ ] Explicit `GameState(...)` invocations are removed from core behavioral tests (used only within the Builder/Fixture definitions).
- [ ] The `shared/core` test suite runs locally in $< 5$ seconds using `./amper test -p jvm`.
- [ ] Detekt linting passes on test files with `CyclomaticComplexity` below baseline due to shortened test methods.
