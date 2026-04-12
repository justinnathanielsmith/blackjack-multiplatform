# 🤖 AI Subagents & Workflows Guide

This guide explains how to leverage the AI subagents and structured workflows built into the Blackjack project to maintain high code quality, performance, and a premium user experience.

---

## 🛠 Subagent Directory (Slash Commands)

Each subagent is a specialized "personality" with a focused mission. They follow a strict **Scan → Select → Implement → Verify → Present** lifecycle.

| Command | Subagent | Mission | Key Focus Areas |
| :--- | :--- | :--- | :--- |
| `/architect`| **Architect** 🏛️ | Structure | Clean Architecture, MVVM/MVI, `commonMain` purity, DRY, DI via `AppGraph`. |
| `/bolt` | **Bolt** ⚡ | Performance | Recomposition, memory leaks, high-frequency frame loops, O(n²) logic. |
| `/bumper` | **Bumper** 📦 | Dependencies | Auditing `libs.versions.toml`, upgrading stable libs, Kotlin/Compose compatibility. |
| `/claude` | **Claude** 🧠 | Reasoning | Complex debugging, deep architectural discussions, multi-phase planning. |
| `/doc` | **Doc** 📝 | Documentation | KDoc blocks, documenting complex logic, maintaining project READMEs. |
| `/eval` | **Eval** 📈 | Quality | Testing strategy, coverage gaps, coroutine/Flow health, native-ready fakes. |
| `/linter` | **Linter** 🎨 | Consistency | `ktlint` & `detekt` violations, hardcoded strings, Modifier ordering. |
| `/palette` | **Palette** 🎨 | UX/UI | Micro-animations, accessibility (`contentDescription`), visual polish, "juice". |
| `/sentinel` | **Sentinel** 🛡️ | Security | Dependency vulnerabilities, data exposure, sensitive logic auditing. |
| `/tutor` | **Tutor** 🧪 | Testing | Missing unit tests, Edge cases, Room test fakes, Coroutine testing. |

---

## 🔄 The Subagent Workflow Lifecycle

When a subagent runs, it follows a standardized process to ensure safety and quality:

1.  **🔍 SCAN**: The agent scans the relevant part of the codebase for one specific problem (e.g., a missing test, a lint violation, or a performance bottleneck).
2.  **🎯 SELECT**: It selects exactly **ONE** high-impact improvement to implement. Subagents index their findings and pick the "low-hanging fruit" or highest-risk issue first.
3.  **🔧 IMPLEMENT**: It applies the change using project-standard patterns (Amper build system, `jj` version control, and idiomatic Kotlin/Compose).
4.  **✅ VERIFY**: It runs a mandatory verification suite:
    - `./amper build -p jvm`
    - `./amper test -p jvm`
    - `./lint.sh` (which runs `ktlint` and `detekt`)
5.  **🎁 PRESENT**: It creates a clean commit/change with a structured Title and Description, explaining the **What**, **Why**, and **Impact**.

### 📓 Journaling (`.claude/journals/`)
Subagents maintain persistent memory to avoid repeating past mistakes.
- **Centralized Journals**: All subagents (e.g., `/architect`, `/bolt`, `/eval`) use `.claude/journals/`.
- **Learning Logs**: They record non-obvious learnings to avoid repeating past mistakes.
- **Project Memory**: These journals help agents understand the specific nuances of *this* codebase.

---

## 🛤 Spec-Driven Development (Conductor Tracks)

For larger feature development, we use **Conductor Tracks**. This is a **Spec-First** approach to engineering.

### Track Structure
Tracks live in `conductor/tracks/<track-name>/` and contain two primary files:

- **`spec.md`**: The source of truth for requirements. It defines the "What" and the "Acceptance Criteria".
- **`plan.md`**: The granular, step-by-step technical implementation plan.

### How to Use
1.  **Pick a Track**: Find an existing track in `conductor/tracks/` or create a new one.
2.  **Read the Spec**: Understand the requirements fully before writing code.
3.  **Follow the Plan**: Execute the steps in `plan.md`, marking them off as you go.
4.  **Test against Spec**: Verify the implementation satisfies the acceptance criteria in `spec.md`.

---

## 🧰 Developer Experience (Tooling)

### Version Control: Jujutsu (`jj`)
This project uses **jj** instead of git. It follows a "working copy as a commit" philosophy.

| Command | Purpose |
| :--- | :--- |
| `jj st` | Check status of the current working copy. |
| `jj diff` | See changes in the current working copy. |
| `jj fix` | **Mandatory**: Auto-formats changed Kotlin files (ktlint). |
| `jj new` | Close the current change and start a new empty one. |
| `jj commit` | Finalize the current change with a message. |
| `jj log` | View the revision graph. |
| `jj git push` | Push bookmarks (branches) to the remote repository. |

### Build System: JetBrains Amper
We use Amper for cross-platform builds. Avoid using `gradlew` directly.

- `./amper build -p jvm` — Fast JVM build for rapid iteration.
- `./amper test -p jvm` — Run all unit tests on the JVM.
- `./lint.sh` — Run static analysis (ktlint + detekt). **Must be green before merging.**

### 📚 Additional Documentation
- [Compose Best Practices](file:///Users/justinsmith/Projects/blackjack/docs/COMPOSE_BEST_PRACTICES.md) — Architectural rules for UI.
- [Turbine Testing](file:///Users/justinsmith/Projects/blackjack/docs/turbine.md) — How we test StateFlows.

---

## ⌨️ How to Invoke a Subagent

To start a subagent run, use the slash command in the chat:

> **Example:** "Hey, run `/linter` on the `sharedUI` module."

The subagent will then autonomously take over the conversation, perform its scan, and present its findings as a `jj` change.

---

## 💡 Best Practices for Working with Subagents

- **One at a time**: Let agents finish their work before starting a new one in the same module to avoid merge conflicts.
- **Review Descriptions**: Agents provide high-quality "Why" and "Impact" sections. They are your primary documentation for the change.
- **Collaborative Refinement**: If an agent proposes a plan you don't like, ask it to "re-scan" or "pivot" to a different approach.
- **Check the Journals**: Read `.claude/journals/*.md` to see what the agents have learned about the codebase over time.
