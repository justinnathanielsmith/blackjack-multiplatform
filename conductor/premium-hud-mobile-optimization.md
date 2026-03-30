# Plan: Premium Blackjack HUD & Mobile UX Optimization

Enhance the game's HUD and information architecture to provide a more immersive "Casino" feel while optimizing for mobile screen real estate and reachability.

## Objective
- Move critical information (Balance) to the top (classic casino style).
- Improve the "Shoe" visual and provide deck/card count feedback.
- Refine the footer for better mobile hit targets and less visual clutter.
- Add contextual labels (Dealer/Player) to the score badges.

## Key Files & Context
- `sharedUI/src/ui/screens/BlackjackScreen.kt`: Main layout orchestration.
- `sharedUI/src/ui/components/Header.kt`: Top-level HUD.
- `sharedUI/src/ui/components/ControlCenter.kt`: Bottom-level HUD and actions.
- `sharedUI/src/ui/components/Shoe.kt`: Deck/Shoe visuals.
- `sharedUI/src/ui/components/ScoreBadge.kt`: Hand value display.

## Implementation Steps

### 1. Header (Top HUD) Refactor
- Update `Header.kt` to accept `balance: Int`.
- Add a central "Vault" pill showing the player's bankroll with a currency icon.
- Add a "Table" info badge showing Stakes (e.g., "$10 / $500").
- Use premium metallic brushes and subtle glass textures.

### 2. Control Center (Bottom HUD) Refactor
- Update `ControlCenter.kt` to remove the Balance display (moved to top).
- Center the "Total Bet" display or move it to a floating pill.
- Optimize vertical padding for mobile devices to maximize table view.
- Update `GameActions.kt` for better spacing and hit targets on phones.

### 3. Shoe & Card Count Enhancement
- Update `Shoe.kt` to include a textual indicator (e.g., "3/6 Decks" or "Cards: 312").
- Improve the 3D "Shoe Box" visual with better depth and material shading.

### 4. Score Badge Improvements
- Update `ScoreBadge.kt` to support an optional label (e.g., "DEALER", "YOUR TURN").
- Enhance the active state animation with a pulsing glow that guides the player's eye.

### 5. Layout Integration
- Update `BlackjackScreen.kt` to pass the necessary state (balance) to the `Header`.
- Adjust the main `Column` to ensure the new HUD elements align perfectly on different screen sizes.

## Verification & Testing
- **Visual Audit**: Verify the HUD looks balanced on phone-sized windows (Portrait).
- **Functionality**: Ensure all buttons (Settings, Rules, etc.) still work.
- **Responsiveness**: Check that the new header doesn't overlap with the dealer's hand on short screens.
- **Accessibility**: Ensure new labels and counts are readable and announced by screen readers.
