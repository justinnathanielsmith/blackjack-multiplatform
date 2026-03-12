# Specification - Betting System & Chip Management

Add a betting phase and chip management to the Blackjack game.

## Requirements
- Players start with a default balance (e.g., $1000).
- A betting phase occurs before cards are dealt.
- Players can place bets in increments (e.g., $10, $50, $100).
- The game only starts when a bet is placed.
- Winnings are paid out at 1:1, or 3:2 for a natural Blackjack.
- Losing a hand deducts the bet from the balance.
- A "Push" returns the bet to the player.

## State Changes
- `GameState` needs `balance: Int` and `currentBet: Int`.
- `GameStatus` needs a `BETTING` state.

## Actions
- `PlaceBet(amount: Int)`: Increases the current bet and decreases the balance.
- `ResetBet`: Clears the current bet and returns it to the balance.
- `Deal`: Transitions from `BETTING` to `PLAYING`.

## UI Components
- **Chip Selector**: Buttons to add different chip values.
- **Balance Display**: Clearly visible current balance.
- **Bet Display**: Current bet on the table.
- **Betting Phase View**: A dedicated layout for placing bets before the game starts.
