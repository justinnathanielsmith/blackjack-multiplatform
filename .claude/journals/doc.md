# Doc's Journal

## 2026-04-12 - HapticsService
**Surprise:** The `HapticsService` interface and its implementations (`AndroidHapticsServiceImpl`, `IosHapticsServiceImpl`, `NoOpHapticsService`) were completely undocumented despite being critical for the "premium" user experience mentioned in project goals.
**Rule:** When documenting services, ensure the interface has the primary documentation and use `@see` or `@inheritDoc` in platform-specific implementations to maintain a single source of truth and consistency.
