# Specification - Premium Visual Refinement

## Goal
Elevate the visual experience of the Blackjack game through advanced materials, lighting, and depth. The goal is to make the table feel like a real physical object (materiality) and the cards feel like they have actual height and weight.

---

## Functional Requirements

### FR-VIS-1: Felt Texture Material
The table background must have a realistic "felt" texture that mimics the look of a premium casino table.
- **Visuals**: A subtle cross-hatch or noise pattern that interacts with the base green gradient.
- **Implementation**: A custom `Brush` or a `Modifier.drawWithCache` that adds a low-opacity, high-frequency noise layer on top of the `FeltGreen` gradient.
- **Blending**: Use a blending mode like `Multiply` or `SoftLight` for the noise to ensure it doesn't look like static.

### FR-VIS-2: Wood Table Rail
The table's perimeter should feature a 3D-looking wood or leather rail (the "bumper") instead of just a flat vignette.
- **Visuals**: A deep, polished wood texture (using `OakMedium` token) with a highlight at the top edge and a shadow at the bottom edge.
- **Implementation**: A `drawRect` with a linear gradient at the edges of the table, or a custom component that renders the rail.

### FR-VIS-3: Embossed Table Markings
The table's markings (the arc, insurance line, logo) should look like they are printed or embossed on the felt, rather than being "stuck on" as a vector.
- **Visuals**: A subtle inner shadow or a slight offset to the markings to simulate the ink sinking into the felt fibers.
- **Blending**: Use `Color.Black.copy(alpha = 0.2f)` for the "emboss" shadow.

### FR-VIS-4: Dynamic Card Shadows
Cards should have shadows that respond to their "height" during animations (e.g., when being dealt).
- **Visuals**: A larger, softer shadow when a card is high (flying from the shoe); a sharper, darker shadow when it lands.
- **Implementation**: In `OverlayCardTable.kt`, modulate the shadow's blur and offset based on the `isFlying` animation state.

### FR-VIS-5: Refined Card Flip "Lift"
The 3D card flip animation should include a slight "lift" (scaling) to simulate the card being picked up and turned over.
- **Visuals**: A subtle increase in scale (1.0 -> 1.05) and shadow blur at the midpoint of the flip (90 degrees).
- **Implementation**: Add an `animateFloat` for scale that peaks at the 90-degree midpoint of the flip transition.

### FR-VIS-6: Glassmorphism HUD Badges
The HUD badges (score, status, title) should use a glassmorphism effect to feel modern yet integrated into the table.
- **Visuals**: A translucent background with a subtle border and a blur effect (if possible on the platform, otherwise a "frost" look).
- **Implementation**: Update `HudTitleBadge` and `HudStatusBadge` in `OverlayCardTable.kt` with a semi-transparent background and a high-contrast thin border (`Color.White.copy(alpha = 0.2f)`).

---

## Out of Scope
- Full 3D rendering (keep it 2.5D with Compose `graphicsLayer`).
- High-fidelity wood grain textures (stay with gradients for performance).
- Ray-traced lighting (stick to gradients and shadows).

---

## Non-Functional Requirements
- **Performance**: Texture generation must be efficient. Use `drawWithCache` to avoid recreating brushes on every frame.
- **Accessibility**: Ensure score badges remain highly readable even with glassmorphism.
- **Adaptability**: Rail and markings must scale correctly across all screen sizes.
