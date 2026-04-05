<div align="center">
  <img src="screenshots/logo.png" width="180" alt="Blackjack Logo">
  <h1>Blackjack Multiplatform</h1>
  <p>A premium, cross-platform Blackjack experience built with <b>Compose Multiplatform</b> and <b>JetBrains Amper</b>.</p>

  [![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-7F52FF.svg?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform-4285F4.svg?style=flat-square&logo=jetpack-compose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
  [![JetBrains Amper](https://img.shields.io/badge/Build-Amper-000000.svg?style=flat-square&logo=jetbrains&logoColor=white)](https://github.com/JetBrains/amper)
  [![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20Desktop-lightgrey.svg?style=flat-square)](https://kotlinlang.org/docs/multiplatform.html)
</div>

---

**Blackjack Multiplatform** is a high-fidelity, production-grade implementation of the classic casino game. It demonstrates a modern, reactive Kotlin Multiplatform (KMP) architecture, supporting **Android**, **iOS**, and **Desktop (JVM)** from a single shared codebase.

### ✨ Key Highlights

*   **Premium Visuals**: Sleek "glassmorphism" UI, dynamic card animations, and adaptive layouts for all screen sizes (Portrait, Compact Landscape, Wide Landscape).
*   **Full Casino Logic**: Comprehensive rules including Split (up to 3 hands), Double-down, Insurance, and Surrender.
*   **Side Bets**: Native support for **21+3** and **Perfect Pairs** side bets with independent resolution logic.
*   **Reactive Core**: A robust serial state machine pattern using `StateFlow` and decoupled `SharedFlow` side effects (spatial audio, haptic vibrations).
*   **Modern Tooling**: Built with **Amper** (no Gradle required), **Decompose** for lifecycle management, and **Jujutsu (jj)** for next-gen version control.

---

## 🏛 Architecture

The project follows a clean, reactive architecture with a unidirectional data flow (UDF).

### Game Status Lifecycle

```mermaid
stateDiagram-v2
    [*] --> BETTING
    BETTING --> DEALING: Place Bet & Deal
    DEALING --> INSURANCE_OFFERED: Dealer shows Ace
    DEALING --> PLAYING: Dealer Shows Other
    INSURANCE_OFFERED --> PLAYING: Decision Made
    PLAYING --> DEALER_TURN: All hands Stand/Bust
    DEALER_TURN --> PLAYER_WON: Player Wins
    DEALER_TURN --> DEALER_WON: Dealer Wins
    DEALER_TURN --> PUSH: Tie
    PLAYER_WON --> BETTING: Reset
    DEALER_WON --> BETTING: Reset
    PUSH --> BETTING: Reset
```

### Module Map

| Module | Responsibility |
| :--- | :--- |
| `shared/core` | **Domain Layer**: `BlackjackStateMachine`, `GameLogic`, immutable models (`GameState`, `Card`, `Hand`). |
| `shared/data` | **Data Layer**: Local persistence using DataStore and Room (KMP). |
| `sharedUI` | **UI Layer**: Shared Compose components, themes (Modern Dark/Gold), and service abstractions (Audio, Haptic interfaces). |
| `androidApp` | **Android Entry**: Platform-specific resources and activity setup. |
| `desktopApp` | **Desktop Entry**: JVM-specific entry point and windowing. |
| `iosApp` | **iOS Entry**: Swift-based entry point and UIViewController integration. |

---

## 🛠 Technology Stack

*   **Language**: Kotlin 1.9.22
*   **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
*   **Build System**: [JetBrains Amper](https://github.com/JetBrains/amper)
*   **State Management**: `StateFlow` + [Decompose](https://github.com/arkivanov/Decompose)
*   **Versioning**: [Jujutsu (jj)](https://github.com/martinvonz/jj)
*   **Serialization**: `kotlinx-serialization`
*   **Concurrency**: `kotlinx-coroutines`
*   **Static Analysis**: `detekt` + `ktlint` + `Kover`

---

## 🚀 Getting Started

This project uses **Amper**. All commands are executed via the `./amper` wrapper.

### Native Execution

| Platform | Command |
| :--- | :--- |
| **Desktop (JVM)** | `./amper run :desktopApp` |
| **Android** | `./amper run :androidApp` |
| **iOS (Simulator)** | `./amper run :iosApp` |

### Development Workflow

```bash
# General Build & Test
./amper build -p jvm                          # Fast JVM build
./amper test -p jvm                           # Run unit tests (JVM)

# Linting & Formatting
./ktlint --format                             # Auto-fix formatting
./lint.sh                                     # Full audit (ktlint + detekt)
jj fix                                        # jj-aware Kotlin formatting
```

---

## 🤖 AI Subagent & Development Workflows

The codebase is optimized for AI-assisted development with specialized subagents.

| Command | Subagent | Responsibility |
| :--- | :--- | :--- |
| `/tutor` | **Test Architect** | Generates missing tests and verifies complex game logic edge cases. |
| `/bolt` | **Performance** | Implements data-driven performance optimizations (Canvas, memory). |
| `/palette` | **UX Specialist** | Refines micro-interactions, animations, and visual polish. |
| `/architect` | **Integrity** | Audits architectural violations and enforces clean layer separation. |
| `/claude` | **Reasoning** | Senior KMP/Compose agent for deep reasoning and debugging. |

For a complete guide, see the [AI Subagent & Workflow Guide](docs/AI_AGENTS_GUIDE.md).

---

Built with ❤️ by the Blackjack Multiplatform Team.
