# Test Quality Refinement

**Goal:** Improve the maintainability, readability, and robustness of the testing suite by addressing DRY (Don't Repeat Yourself) boilerplates and SOLID principle violations.

## Context
The current test suite in `shared/core` has grown rapidly. While providing good coverage, many tests suffer from code duplication (especially around `GameState` instantiation and Turbine stream consumption) and poor separation of concerns (verifying state transformations and side-effects in the same assertions).

This track will establish structured testing patterns, particularly Test Data Builders, custom internal DSLs/assertions, and parameterized suites to improve the developer experience and make the testing boundaries cleaner.

## Documents
- [Specification](file:///Users/justinsmith/Projects/blackjack/conductor/tracks/test-quality-refinement/spec.md): Defines the core rules for testing syntax and architectural boundaries for the suite.
- [Execution Plan](file:///Users/justinsmith/Projects/blackjack/conductor/tracks/test-quality-refinement/plan.md): The step-by-step sprint tasks to be completed.
