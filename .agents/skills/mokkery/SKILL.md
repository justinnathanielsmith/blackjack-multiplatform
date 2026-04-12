---
name: mokkery
description: >
  Expertise in using the Mokkery mocking library for Kotlin Multiplatform. Use this when writing
  or fixing mocks in KMP tests. It provides comprehensive guidance on mocking, spying,
  stubbing, and verification in a cross-platform context.
---

# Mokkery: Kotlin Multiplatform Mocking

## Overview
Mokkery is a Kotlin Multiplatform (KMP) mocking library that uses a **Kotlin Compiler Plugin**. It is highly compatible with Kotlin/Native (iOS) and Kotlin 2.0+, providing an experience similar to MockK but without the reliance on reflection.

## When to Use
- **KMP Shared Logic:** When testing code in `commonTest` that runs on multiple platforms (JVM, iOS, Android).
- **Interface Mocking:** When you need to substitute dependencies that are defined as interfaces.
- **Suspend Functions:** When you need to mock coroutines-based logic.

## Core Concepts
- **Compiler Plugin:** No KSP or reflection. It intercepts function calls at compile-time.
- **Interfaces First:** Mokkery works best with interfaces. Concrete classes must be `open` (or use the `all-open` plugin).
- **`mock<T>()`:** Creates a mock instance.
- **`spy<T>()`:** Creates a spy instance.

## Common Workflows

### 1. Mocking Regular Functions
Use `every { ... } returns ...` to stub behavior.

```kotlin
val mock = mock<UserRepository>()
every { mock.getName("1") } returns "Alice"
```

### 2. Mocking Suspend Functions
Use `everySuspend { ... } returns ...` for suspending functions.

```kotlin
everySuspend { mock.getAge("1") } returns 30
```

### 3. Argument Matchers
- `any()`: Matches any argument.
- `eq(value)`: Matches a specific value.
- `matching { ... }`: Custom logic for matching.

```kotlin
every { mock.getName(any()) } returns "Unknown"
every { mock.saveUser(matching { it.name == "Alice" }) } returns Unit
```

### 4. Verification
- `verify { ... }`: Verify a regular call.
- `verifySuspend { ... }`: Verify a suspend call.
- `verifyNoCalls { ... }`: Verify no calls were made.
- `verifyNoMoreCalls { ... }`: Verify all calls were accounted for.

```kotlin
verify { mock.getName("1") }
verifySuspend { mock.getAge("1") }
```

### 5. Verification Order
Use `verifyOrder { ... }` to ensure calls happened in a specific sequence.

```kotlin
verifyOrder {
    mock.getName("1")
    mock.getAge("1")
}
```

### 6. Complex Stubs (Answers)
Use `calls { ... }` to execute logic during a call.

```kotlin
every { mock.getName(any()) } calls { (id: String) -> "User_$id" }
```

## 7. Spying
Use `spy<T>(instance)` to wrap a real object.

```kotlin
val realService = RealService()
val spied = spy(realService)
every { spied.getName(any()) } returns "Mocked"
```

## Boundaries
- **Mocking Classes:** Concrete classes must be `open`. If you can't open them, wrap them in an interface.
- **Reflection:** Avoid using reflection-based mocking libraries (MockK/Mockito) in `commonTest`.
- **KMP Compatibility:** Ensure all mocks and verifications are defined in `commonTest` where possible.

## Technical Details
- **Dependency:** Add `dev.mokkery` plugin to `build.gradle.kts`.
- **Kotlin 2.0+:** Fully supports Kotlin 2.0 and above.
- **Amper:** Currently, Mokkery is most easily used in Gradle-backed KMP projects. For Amper, verify plugin support or use hand-rolled fakes if the compiler plugin cannot be applied.
