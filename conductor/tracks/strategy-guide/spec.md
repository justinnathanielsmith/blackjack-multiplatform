# Specification - Blackjack Strategy Guide

## Overview
The Strategy Guide provides players with mathematically optimal moves based on common "Basic Strategy" charts. It is accessible via a new "Strategy" button on the main gameplay screen.

## UI Requirements

### 1. Navigation
- **3 Tabs**: Hard, Soft, Pairs.
- **Persistent Header**: Dealer upcard (2, 3, 4, 5, 6, 7, 8, 9, 10, A) across the top row.
- **Row Headers**: Player hand totals or pairs down the left column.

### 2. Tab Content
#### **Tab 1: Hard Hands**
| Player | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | A |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **8 or less** | H | H | H | H | H | H | H | H | H | H |
| **9** | H | D | D | D | D | H | H | H | H | H |
| **10** | D | D | D | D | D | D | D | D | H | H |
| **11** | D | D | D | D | D | D | D | D | D | H |
| **12** | H | H | S | S | S | H | H | H | H | H |
| **13** | S | S | S | S | S | H | H | H | H | H |
| **14** | S | S | S | S | S | H | H | H | H | H |
| **15** | S | S | S | S | S | H | H | H | H | H |
| **16** | S | S | S | S | S | H | H | H | H | H |
| **17+** | S | S | S | S | S | S | S | S | S | S |

#### **Tab 2: Soft Hands (Ace + X)**
| Player | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | A |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **A,2** | H | H | H | D | D | H | H | H | H | H |
| **A,3** | H | H | H | D | D | H | H | H | H | H |
| **A,4** | H | H | D | D | D | H | H | H | H | H |
| **A,5** | H | H | D | D | D | H | H | H | H | H |
| **A,6** | H | D | D | D | D | H | H | H | H | H |
| **A,7** | S | D | D | D | D | S | S | H | H | H |
| **A,8** | S | S | S | S | S | S | S | S | S | S |
| **A,9** | S | S | S | S | S | S | S | S | S | S |

#### **Tab 3: Pairs**
| Player | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | A |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **A,A** | P | P | P | P | P | P | P | P | P | P |
| **10,10** | S | S | S | S | S | S | S | S | S | S |
| **9,9** | P | P | P | P | P | S | P | P | S | S |
| **8,8** | P | P | P | P | P | P | P | P | P | P |
| **7,7** | P | P | P | P | P | P | H | H | H | H |
| **6,6** | P | P | P | P | P | H | H | H | H | H |
| **5,5** | D | D | D | D | D | D | D | D | H | H |
| **4,4** | H | H | H | P | P | H | H | H | H | H |
| **3,3** | P | P | P | P | P | P | H | H | H | H |
| **2,2** | P | P | P | P | P | P | H | H | H | H |

### 3. Legend & Colors
- **H (Hit)**: Green (`ChipGreen`)
- **S (Stand)**: Red (`TacticalRed`)
- **D (Double)**: Gold/Yellow (`PrimaryGold`)
- **P (Split)**: Purple (`ChipPurple`)

### 4. Interactive Elements
- **Tabs**: Smooth transition between Hard, Soft, and Pairs views.
- **Highlighting**: If the game is in progress, the guide should highlight the current cell corresponding to the player's active hand and the dealer's upcard.

## Technical Specifications
- **Component**: `StrategyGuideComponent` (Decompose)
- **UI**: `StrategyGuideScreen` using `ModalBottomSheet` or a separate full-screen layer.
- **Data Model**: `sealed class StrategyTab`, `data class StrategyCell(val action: StrategyAction)`.

## Out of Scope
- **Surrender**: Not currently implemented in the game core.
- **Specific Rule Variations**: (e.g., Doubling after splitting) - using standard S17 (Dealer stands on Soft 17) strategy.
