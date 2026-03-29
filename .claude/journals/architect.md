# Architect Journal

## 2026-03-29 - BalanceService Interface Extraction Was Zero-Friction
**Learning:** When extracting an interface from a concrete class, naming the interface identically to the old class means zero consumer changes — imports, type annotations, and AppGraph declarations all stay untouched. The only files that change are the data layer itself and tests that directly construct the concrete type. This repo's `SettingsRepository` / `DataStoreSettingsRepository` split is the canonical pattern; every future service extraction should mirror it exactly.
**Action:** Before extracting a service interface, check whether keeping the interface name identical to the old concrete class avoids a cascade of consumer edits. If the class is already named as a noun/capability (`BalanceService`, `AudioService`) rather than an impl (`BalanceServiceImpl`), reuse the name for the interface.
