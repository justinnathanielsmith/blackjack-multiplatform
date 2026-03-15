# Specification - UI/UX Modernization

## Goal
Elevate the Blackjack experience beyond functional components into a cohesive, premium interface. Focus on "Containment," "Semantic Clarity," and "Visual Harmony."

---

## Functional Requirements

### FR-UX-1: Boundary-Aware Score Badges
Score badges (player and dealer) must be contained within their respective hand areas.
- **Placement**: Anchor to top-right corner *inside* the hand container padding.
- **Visibility**: Must remain visible regardless of card count (avoid overlapping with the last card).
- **Style**: Use existing `ScoreBadge` styles (Active, Dealer, Waiting).

### FR-UX-2: Standardized State Labels
State labels (DEALER, ACTIVE, WAITING) must follow a uniform positioning pattern.
- **Position**: Top-center of the hand area, straddling the top border.
- **Consistency**: The "DEALER" label must match the "ACTIVE/WAITING" label placement (centered).

### FR-UX-3: Contained Betting Chips
Betting chips must not overlap container borders or adjacent hand areas.
- **Placement**: Anchor the chip inside the bottom-center of each hand's bounding box.
- **Z-Index**: Chips should appear slightly below or behind the cards to ensure rank/suit legibility is never compromised.

### FR-UX-4: Semantic Action Bar
The bottom control bar must communicate action intent through both color and text.
- **Semantic Colors**: 
  - `Hit`: Green or vibrant Blue.
  - `Stand`: Red.
  - `Double Down`: Premium Gold/Yellow.
- **Labels**: Add explicit text labels (HIT, STAND, DOUBLE) below or alongside icons inside the circular buttons.
- **Accessibility**: Support tap targets of at least 48dp (currently met by circles).

### FR-UX-5: Premium Visual Polish
Refine header and asset aesthetics.
- **Header Alignment**: Vertical center alignment for Balance text and the settings/utility icon row.
- **Utility Icons**: Reduce visual weight of the "Lightning" (auto-deal) icon if active to prevent it from out-shining game content.
- **Card Back Textures**: Replace the checkerboard pattern with a more premium geometric or solid-gradient design.

---

## Non-Functional Requirements
- **Responsive Layout**: Adjustments must work across PORTRAIT, LANDSCAPE_COMPACT, and LANDSCAPE_WIDE.
- **Performance**: Heavy use of transparency or complex gradients in the Action Bar should not drop frames on JVM/Android.
- **Consistency**: Use `Dimensions.kt` constants for all new offsets and padding.
