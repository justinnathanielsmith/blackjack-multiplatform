# Product Guidelines - Blackjack Multiplatform

Guidelines for maintaining a "premium" feel and consistent architecture.

## UI/UX Standards
- **Juicy Feedback**: Every game event should have a visual and/or auditory response.
- **Animations**: Use `Canvas` + `withFrameNanos` for performance-heavy effects (e.g., confetti).
- **Adaptive Layouts**: Follow patterns in `MemoryMatch` to ensure the UI scales gracefully from mobile to desktop.
- **Theming**: Use a curated color palette (e.g., FeltGreen, ModernGold).

## Coding Standards
- **Idiomatic Kotlin**: Prefer functional transformations and concise syntax.
- **Platform Abstraction**: Keep platform-specific code in `src@<platform>` or behind service interfaces.
- **State Management**: The UI should be a pure function of the `BlackjackStateMachine` state.

## Educational Standards
- **Explanatory Comments**: Use TIP/NOTE/IMPORTANT alerts to highlight key learnings.
- **Walkthroughs**: Required for any new core feature or major refactor.
