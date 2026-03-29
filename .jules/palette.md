## 2024-05-18 - Added Semantics and Button Roles to Custom Composables
**Learning:** In Compose Multiplatform, using `.clickable { ... }` on a `Box` or `Canvas` to create a custom button does not automatically expose it as a button to screen readers. Elements like `BetSpot`, `BetChip`, and `CasinoButton` were functioning interactively but lacked correct accessibility roles and descriptions.
**Action:** Always include `role = Role.Button` in `.clickable(role = Role.Button) { ... }` when creating custom button components. Additionally, for interactive elements that are essentially custom controls (like betting spots), ensure a `semantics { contentDescription = "..." }` is provided so screen readers describe the action (e.g., "Tap to place bet").
## 2024-05-19 - Added Proper Semantic Roles to Interactive Header, Settings, and Strategy Elements
**Learning:** In Compose Multiplatform, using `.clickable { ... }` on a `Box` to create custom tabs or buttons requires specific semantic roles for screen readers. Using `Role.Tab` for tab components (like Strategy Tabs) and `Role.Button` for header icons/settings rows properly informs screen readers of the component's interaction type.
**Action:** Always assign the most specific semantic role (`Role.Button`, `Role.Tab`, etc.) in `.clickable(role = Role.X) { ... }` when building custom interactive components.
## 2024-05-20 - Enlarge Touch Targets and Semantic Grouping for Form Rows
**Learning:** When using controls like `Switch` or dropdown triggers inside a `Row`, placing the click/toggle listener only on the small control creates a poor UX for touch users and fragments semantics for screen readers (they read the text label separately from the switch).
**Action:** Always apply `Modifier.toggleable` or `Modifier.clickable` to the entire `Row` containing the label and the control. For `Switch`, pass `null` to its `onCheckedChange` to prevent duplicate semantic reading and let the Row handle the interaction.
## 2024-05-21 - Restoring Focus Indicators for Custom Interactive Components
**Learning:** In Jetpack Compose, when you build a custom interactive element (like `CasinoButton`, `BetChip`, or `GameActionButton`) using a `Box` or `Canvas` with `.clickable` and pass `indication = null` to completely remove ripple effects, you inadvertently strip away the default visual focus state necessary for keyboard/D-pad navigation. This renders the application inaccessible for keyboard users, as they cannot tell which element is currently focused.
**Action:** When removing ripples via `indication = null`, track the element's focus state manually using `val isFocused by interactionSource.collectIsFocusedAsState()`. Apply a visual indicator, such as a prominent `Modifier.border()`, when `isFocused` is true. This ensures your components remain accessible without disrupting the desired interaction visual design.

## 2025-03-23 - Hardcoded Colors Break Dialog Themes
**Learning:** Hardcoding text colors (like `Color.Black`) inside custom Material 3 dialogs (like `AlertDialog`) completely overrides the default `LocalContentColor`, making the text unreadable when the dialog uses a dark background or when the app is in dark mode.
**Action:** Always rely on default Material theming and `LocalContentColor` (or explicitly set `titleContentColor` and `textContentColor` in the dialog definition) to ensure readability across all themes instead of forcing inline text colors.

## 2025-03-24 - Screen Reader Announcements for Dynamic UI Overlays
**Learning:** When dynamic UI overlays (like `GameStatusMessage`) appear with important game state changes (e.g., win/loss, net payout), screen readers do not automatically read the contents unless explicitly instructed. This leaves visually impaired users unaware of game outcomes until they manually explore the screen.
**Action:** Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` combined with an explicit `contentDescription` on the top-level container of dynamic overlays to ensure screen readers automatically announce crucial status updates when they appear or change.

## 2025-03-24 - Screen Reader Announcements for Individual Component Outcomes
**Learning:** While a top-level `GameStatusMessage` can announce overall game state changes, individual components (like `HandOutcomeBadge` for each split hand) that appear dynamically also need their own semantics to announce their specific results. If not explicitly tagged, screen readers may miss these granular, component-specific updates entirely.
**Action:** Always verify if dynamic UI overlays or badges that contain unique, per-component information (such as individual hand payouts) use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` along with a computed `contentDescription`, even if there's a broader screen-level status announcement.

## 2025-03-26 - Screen Reader Announcements for Dynamic Component Values (Scores)
**Learning:** In Compose Multiplatform, UI elements like a blackjack `ScoreBadge` that dynamically pop in or continuously change values aren't automatically read aloud by screen readers as they appear. This leads to a situation where visual users see a hand's live score (e.g., jumping from 10 to 18), but screen reader users get no auditory feedback of the current total unless they manually re-focus the badge.
**Action:** Append `Modifier.semantics { liveRegion = LiveRegionMode.Polite; contentDescription = "..." }` to dynamically appearing/updating individual components like score badges. This ensures the accessibility service announces the state change (like "Score 18" or "Bust, score 24") right as it happens, keeping audio-dependent users in the loop automatically.

## 2025-03-27 - Exposing Selected State for Custom Tabs and Toggles
**Learning:** When creating custom Tabs or Toggle buttons using a `Box` or `Row`, simply using `.clickable(role = Role.Tab)` or `Role.Button` fails to communicate the actual active/selected state to screen readers. Blind users hear the role but cannot perceive which tab is active or if a feature like Auto-Deal is on/off.
**Action:** Use `Modifier.selectable(selected = ..., role = Role.Tab)` for custom tabs and `Modifier.toggleable(value = ..., role = Role.Switch)` for custom toggles instead of `.clickable()`. This inherently exposes the current boolean state to accessibility services.

## 2026-03-29 - LiveRegion for Financial Data and Conditional UX Logic
**Learning:** High-impact micro-UX is often about "delighting" the user by removing frictions or providing automatic feedback. Updating the "Reset" bet button from an emoji (`❌`) to premium text and dynamically disabling it when no bet is active prevents "invalid" interaction attempts. Furthermore, critical financial updates like total balances are easily missed by screen readers if not specifically marked.
**Action:** 
1. Use `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` on labels that represent dynamic game values (e.g., current Balance) so users are automatically notified of wins/losses. 
2. Prefer text over emojis for primary action buttons in "premium" styled apps to maintain consistent typography.
3. Always calculate and pass the `enabled` state to custom buttons (like `CasinoButton`) based on the current domain state to provide clear visual and functional feedback.

## 2026-03-29 - Animated Selectable Chips and Accessible Deal Action
**Learning:** Using `Modifier.selectable(selected = ...)` on custom components like `BetChip` provides intrinsic "Selected" semantics for screen readers without needing manual state description strings. Additionally, animating size changes with `animateDpAsState` significantly improves the perceived "juice" of the interaction.
**Action:** Prefer `Modifier.selectable` for radio-button style selection in custom Compose components. Always animate layout-impacting state changes (like selection size) to avoid jarring UI jumps.

## 2026-03-29 - Contextual Seat Labels for Multi-Hand Accessibility
**Learning:** In multi-hand games (like 2-3 seats), using generic labels like "Tap to place bet" for all seats makes them indistinguishable to screen reader users. Providing spatial labels ("Left", "Center", "Right") significantly improves navigation and clarity.
**Action:** Always provide context-aware labels (position-based) for repeated interactive slots. Pass these labels to custom components and incorporate them into `contentDescription` templates.
## 2026-03-29 - ChipStack Pulse Animation
**Learning:** Reading `Animatable.value` inside a `graphicsLayer { ... }` block defers state reads to the rendering layer phase, preventing expensive full composable recompositions when adding quick micro-interactions (like a chip stack scaling pulse).
**Action:** Use `graphicsLayer { scaleX = anim.value }` instead of passing the animated value directly to a modifier or drawing it outside a lambda.
