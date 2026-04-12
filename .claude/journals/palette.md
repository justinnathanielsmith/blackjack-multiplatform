# Palette Journal

_Micro-UX and accessibility learnings discovered during the visual polishing of Blackjack._

---

## 2026-04-12 - Header Interaction Feedback
**Learning:** Static icons in the header can be improved by adding snappy scale feedback and hover states without using the default ripple, which often clashes with custom glass/metallic backgrounds.
**Action:** Use `animateFloatAsState` with a spring spec and `graphicsLayer` for light-weight, performant interaction feedback on small UI elements.

## 2026-03-29 - LiveRegion for Financial Data and Conditional UX Logic
**Learning:** High-impact micro-UX is about removing frictions. Updating the "Reset" bet button from an emoji (❌) to premium text and dynamically disabling it when no bet is active prevents invalid interaction attempts. Critical financial updates like total balances are easily missed by screen readers if not specifically marked.
**Action:**
1. Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on labels representing dynamic game values (e.g., Balance).
2. Prefer text over emojis for primary action buttons to maintain consistent typography.
3. Calculate and pass the `enabled` state to custom buttons based on domain state.

## 2026-03-29 - Contextual Seat Labels for Multi-Hand Accessibility
**Learning:** In multi-hand games, using generic labels like "Tap to place bet" for all seats makes them indistinguishable to screen reader users.
**Action:** Provide position-based labels ("Left", "Center", "Right") and incorporate them into `contentDescription`.

## 2026-03-29 - Animated Selectable Chips and Accessible Deal Action
**Learning:** Using `Modifier.selectable(selected = ...)` provides intrinsic "Selected" semantics for screen readers. Animating size changes with `animateDpAsState` improves interaction "juice".
**Action:** Prefer `Modifier.selectable` for radio-button style selection. Always animate layout-impacting state changes to avoid jarring UI jumps.

## 2026-03-29 - ChipStack Pulse Animation
**Learning:** Reading `Animatable.value` inside a `graphicsLayer { ... }` block defers state reads to the rendering layer phase, preventing expensive recompositions.
**Action:** Use `graphicsLayer { scaleX = anim.value }` instead of passing animated values directly to modifiers.

## 2026-03-27 - Exposing Selected State for Custom Tabs and Toggles
**Learning:** When creating custom Tabs or Toggles using a `Box`, simply using `.clickable` fails to communicate the active/selected state to screen readers.
**Action:** Use `Modifier.selectable(selected = ..., role = Role.Tab)` or `Modifier.toggleable(value = ..., role = Role.Switch)`.

## 2026-03-26 - Screen Reader Announcements for Dynamic Component Values (Scores)
**Learning:** Score badges that dynamically pop in or change values aren't automatically read aloud as they appear.
**Action:** Append `Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "..." }` to dynamically updating components like score badges.

## 2025-03-24 - Screen Reader Announcements for Dynamic UI Overlays
**Learning:** Dynamic UI overlays (like `GameStatusMessage`) are not automatically read by screen readers unless explicitly instructed.
**Action:** Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` combined with an explicit `contentDescription` on the top-level container.

## 2025-03-23 - Hardcoded Colors Break Dialog Themes
**Learning:** Hardcoding text colors (like `Color.Black`) inside custom Material 3 dialogs overrides the default `LocalContentColor`, making text unreadable in dark mode.
**Action:** Always rely on default Material theming and `LocalContentColor`.

## 2024-05-21 - Restoring Focus Indicators for Custom Interactive Components
**Learning:** When using `.clickable(indication = null)` to remove ripple effects, you strip away the default visual focus state for keyboard/D-pad navigation.
**Action:** Track focus state manually using `val isFocused by interactionSource.collectIsFocusedAsState()` and apply a visual indicator (e.g., a border) when true.

## 2024-04-02 - Correct Semantic Roles for Selectable Elements
**Learning:** For mutually exclusive options, `Role.RadioButton` must be used so screen readers correctly announce when an element is selected.
**Action:** Explicitly pass `role = Role.RadioButton` to `Modifier.selectable` for chips or custom tabs.
