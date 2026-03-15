# Specification - Side Bets

## Overview
Side bets are optional wagers placed at the start of a round, alongside the main bet. They are independent of the main game outcome and offer higher payouts based on early card combinations.

## Requirements

### 1. Supported Side Bets

#### **21+3 (Poker Hands)**
Uses the player's first two cards and the dealer's upcard.
- **Suited Triple**: Three identical cards (e.g., three Kings of Hearts) - Pays 100:1.
- **Straight Flush**: Three cards in sequence and same suit - Pays 40:1.
- **Three of a Kind**: Three cards of the same rank - Pays 30:1.
- **Straight**: Three cards in sequence - Pays 10:1.
- **Flush**: Three cards of the same suit - Pays 5:1.

#### **Perfect Pairs**
Uses the player's first two cards.
- **Perfect Pair**: Two identical cards (e.g., two 10s of Spades) - Pays 25:1.
- **Colored Pair**: Same rank, same color (e.g., 10 of Diamonds, 10 of Hearts) - Pays 12:1.
- **Mixed Pair**: Same rank, different colors (e.g., 10 of Hearts, 10 of Spades) - Pays 5:1.

#### **Insurance** (Existing)
Already implemented but should be integrated into the side bet logic for consistency.
- Pays 2:1 if the dealer has Blackjack.

### 2. Gameplay Flow
- **Placement**: Side bets must be placed during the `BETTING` phase.
- **Resolution**: Settled immediately after the initial deal (before player actions).
- **Independence**: Winning/losing a side bet does not affect the main hand.

## Technical Specifications

### Data Model
- `enum class SideBetType`: `TWENTY_ONE_PLUS_THREE`, `PERFECT_PAIRS`, `INSURANCE`.
- `GameState`: Needs a way to store side bets (e.g., `sideBets: PersistentMap<SideBetType, Int>`).

### State Machine Changes
- `GameAction.PlaceSideBet(type: SideBetType, amount: Int)`
- `GameAction.ResetSideBets`
- `BlackjackStateMachine.handleDeal()` should evaluate side bets and update balance/effects.

### UI Requirements
- **Betting Screen**: Add slots for side bets near the main betting area.
- **Animations**: Visual feedback for winning/losing side bets (e.g., floating payout text).

## Out of Scope
- Progressive jackpots.
- Other niche side bets (e.g., Buster Blackjack, Lucky Ladies).
