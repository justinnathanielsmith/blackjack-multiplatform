# Blackjack Multiplatform: Game Design & Experience Reference

This document provides a comprehensive overview of the Blackjack Multiplatform project's gameplay, visuals, and technical flow. It is designed as a "High-Signal" reference for AI agents and game designers to understand the current state of the app and identify opportunities for UI/UX and gameplay improvements.

---

## 1. Core Vision & Experience Goals

### Vision
A premium, "high-roller" Blackjack experience that serves as a reference implementation for modern Kotlin Multiplatform (KMP) and Compose Multiplatform development.

### Experience Goals (The "Juice")
*   **Tactile Realism:** The UI should feel like a physical casino table (felt, wood, cards, chips).
*   **High-Impact Feedback:** Every action (betting, dealing, winning) must have a visual, auditory, or haptic response.
*   **Clarity & Elegance:** A clean, distraction-free interface that focuses on the cards and the betting action.
*   **Performance:** Silky-smooth animations (60+ FPS) across mobile and desktop.

---

## 2. Gameplay Mechanics & Rules

### Core Rules
*   **Decks:** Multi-deck shoe (configurable, typically 6-8 decks).
*   **Dealer Behavior:** Dealer stands on all 17s (configurable to hit on Soft 17).
*   **Payouts:** 
    *   Standard Win: 1:1
    *   Natural Blackjack: 3:2 (Standard casino payout).
*   **Player Options:**
    *   **Hit/Stand:** Standard play.
    *   **Double Down:** Double the bet and receive exactly one more card.
    *   **Split:** Split a pair into two separate hands (Multi-hand support).
    *   **Insurance:** Offered when the dealer shows an Ace.
    *   **Surrender:** Optional early exit to reclaim half the bet.

### Side Bets
The game supports two popular casino side bets, evaluated immediately after the initial deal:
*   **Perfect Pairs:**
    *   Perfect Pair (Same suit, same rank): 25:1
    *   Colored Pair (Different suit, same color, same rank): 12:1
    *   Mixed Pair (Different suit, different color, same rank): 5:1
*   **21+3 (Poker-style 3-card hand using player's 2 cards + dealer's upcard):**
    *   Suited Triple: 100:1
    *   Straight Flush: 40:1
    *   Three of a Kind: 30:1
    *   Straight: 10:1
    *   Flush: 5:1

---

## 3. Visual Language & UI/UX

### Aesthetic: "Modern High-Roller"
*   **Table Surface:** Deep "Felt Green" with a radial warm center. Subtle fiber/linen texture. 
*   **Table Markings:** Gold-embossed betting and insurance arcs. Painted-on-felt appearance for betting spots.
*   **Environment:** A heavy vignette and physical wood table rails create a "dimly lit casino lounge" atmosphere.
*   **Cards:** High-fidelity 2D cards with dynamic drop shadows. Organic, deterministic random rotations to avoid a "digitally perfect" look.

### Animations & "Juice"
*   **3D Casino Toss:** Cards are dealt from the top-right (off-screen shoe) with a bouncy, spring-based offset, scale, and rotation.
*   **Hand Layout:** Borderless containers. Cards are fanned in a subtle arc and diagonally staggered for a physical feel.
*   **Active States:** Radial glows on the felt behind the active hand. Score badges and status indicators lift off the table with shadows.
*   **Feedback:** 
    *   Confetti/Particle effects for big wins.
    *   Bouncy "Game Status" banners (e.g., "Player Bust", "Dealer Stands").
    *   Integrated audio for card flips, chip clicks, and outcome resolutions.

---

## 4. Game Flow & Interaction

### The Game Loop
1.  **Splash/Loading:** High-quality entry into the app.
2.  **Betting Phase:** 
    *   The player sits at the "Table Surface".
    *   Chips are selected from a bottom rail and placed into betting slots.
    *   No modal dimming; the betting phase is integrated into the physical table.
3.  **The Deal:**
    *   Dealer tosses two cards to the player and two to themselves (one face-down).
    *   Side bets are resolved instantly with visual badges.
4.  **Player Turn:**
    *   Active hand is highlighted.
    *   Action buttons (Hit, Stand, etc.) appear in the "table rail" area.
5.  **Dealer Turn:**
    *   Hole card is flipped.
    *   Dealer hits until threshold (17).
6.  **Resolution:**
    *   Payouts are calculated and chips are moved (visually/conceptually) to the player's balance.
    *   Outcome banners display results.
7.  **Next Round:** Table clears, and the loop restarts.

---

## 5. Technical Architecture (for Context)

*   **State Machine:** Pure logic in , making the UI a "pure function" of the game state.
*   **Compose Multiplatform:** Shared UI code across Android, iOS, and Desktop.
*   **Custom Layouts:** Specialized `Layout` algorithms for fanning cards and positioning table elements.
*   **Graphics:** Heavy use of `Canvas` and `drawWithCache` for performance-optimized rendering of the table and effects.

---

## 6. Current Opportunities for Improvement (Designer Notes)

*   **Chip Physics:** Enhance the visual of chips stacking and moving on the table.
*   **Dealer Presence:** Add more visual "personality" to the dealer (e.g., a hand reaching from the top to flip cards).
*   **Social/Multiplayer:** Visualizing "other players" at the table to increase immersion.
*   **Atmospheric Effects:** Subtle smoke, lighting shifts, or table reflections to deepen the "High-Roller" mood.
*   **Education:** Better "Strategy Guide" integration that feels like part of the table (e.g., a "cheat sheet" card).
