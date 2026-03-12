# Tech Stack - Blackjack Multiplatform

This project leverages the modern Kotlin ecosystem for multiplatform development.

## Core Frameworks
- **Kotlin Multiplatform (KMP)**: Sharing logic across all platforms.
- **Compose Multiplatform**: Shared UI framework for Android, iOS, Desktop, and Web.
- **JetBrains Amper**: Modern build configuration tool (replacing Gradle scripts with YAML).

## Version Control
- **Jujutsu (JJ)**: A Git-compatible version control system with a focus on ease of use and powerful history manipulation.

## Architecture
- **State Machine**: Centralized game logic in `shared/core` using a state-driven approach.
- **Dependency Injection**: Simple graph-based DI in `sharedUI/di`.
- **Service Layer**: Platform-abstracted services for Audio and Haptics.

## External Dependencies
- **Kotlinx Coroutines**: For asynchronous logic.
- **Kotlinx Serialization**: (If needed for state persistence).
- **Compose Material 3**: UI components.
- **Skiko**: Graphics engine for Compose Multiplatform.
