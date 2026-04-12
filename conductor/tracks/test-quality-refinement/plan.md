# Execution Plan: Test Quality Refinement

**Target Audience:** Junior / Mid-level Developers
**Objective:** Incrementally refactor the test suite without altering production code. Run `./amper test -p jvm` after every task to guarantee functionality.

## Sprint Tasks

### Phase 1: Foundation (Builders & Extensions)
1. **Create `GameStateFixtures`**
   - Create a file `shared/core/test/util/GameStateFixtures.kt`.
   - Implement factory functions capturing common game milestones: `defaultBettingState()`, `defaultPlayingState()`, etc.
   - Utilize a DSL or builder pattern so test properties (like specific hands) can be injected without knowing the full constructor structure.

2. **Create Turbine Extension Utilities**
   - In `shared/core/test/util/StateMachineTestUtils.kt`.
   - Create an extension on `BlackjackStateMachine` called `assertTransition(action, assertionBlock)`.
   - The utility must handle the `state.test { awaitItem(); dispatch(action); val state = awaitItem(); assertionBlock(state); cancelAndIgnoreRemainingEvents() }` internally.

### Phase 2: Core Refactoring (Reducing Boilerplate)
1. **Refactor `BettingPhaseTest.kt`**
   - Utilize the new `GameStateFixtures` and `assertTransition` helpers.
   - Collapse repetitive "amount too low / amount too high" tests into a `listOf(0, -50, 1500).forEach` block.

2. **Refactor `SplitTest.kt`**
   - Remove occurrences of raw `persistentListOf(hand(Rank.ACE, Rank.ACE).copy(bet = 100), ...)`.
   - Switch entirely to the unified Fixture generator.
   - Ensure the updated tests precisely emphasize what properties of the State split mechanics affect (e.g., activeHandIndex, balanced deduction) without testing completely unrelated properties.

### Phase 3: Separation of Concerns (SRP)
1. **Refactor `PlayerActionLogicTest.kt`**
   - Review functions like `doubleDown_updatesStateCorrectly_andAdvancesTurn`.
   - Split complex validations into isolated functional tests. Assert pure data transformation independently from the side-effect validation mapping.
   - Verify that test names strictly match their precise functional expectation.

2. **Review Middlewares**
   - Ensure `GameEffectsFlowTest` and similar middleware interceptors do not redundantly assert pure state transitions that are already heavily covered by reducer tests.
   - Focus middleware tests solely on flow mapping/filtering boundaries.

### Phase 4: Final Validation
1. **Execute CI Emulation**
   - Run the `./lint.sh` script. Ensure Detekt does not flag new complex structures inside tests.
   - Run the full cross-platform build test via `./amper test`.
   - Verify all existing edge cases remain tested and code coverage is intact using Kover (if available).
