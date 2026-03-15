# Implementation Plan - Blackjack Strategy Guide

Add a comprehensive strategy guide to help players make optimal decisions. Implementation involves defining the strategy data, creating a reusable chart UI, and integrating it into the main gameplay screen.

---

## Step 1: Define Strategy Domain Models (`shared/core/src/`)

### 1a: Create `StrategyLogic.kt`
Define enums and data structures for the strategy guide.

```kotlin
enum class StrategyAction {
    HIT, STAND, DOUBLE, SPLIT
}

sealed class StrategyTab {
    data object Hard : StrategyTab()
    data object Soft : StrategyTab()
    data object Pairs : StrategyTab()
}

data class StrategyCell(val action: StrategyAction)
```

### 1b: Implement `StrategyProvider`
A singleton or helper to provide the full grid data for each tab.
- `getHardStrategy(): List<List<StrategyAction>>`
- `getSoftStrategy(): List<List<StrategyAction>>`
- `getPairsStrategy(): List<List<StrategyAction>>`

---

## Step 2: Create Strategy UI Components (`sharedUI/src/ui/components/`)

### 2a: `StrategyChartCell`
A small box with background color based on `StrategyAction`.
- HIT: `ChipGreen`
- STAND: `TacticalRed`
- DOUBLE: `PrimaryGold`
- SPLIT: `ChipPurple`

### 2b: `StrategyChart`
A grid component that renders a `StrategyTab`.
- Responsive layout using `LazyColumn` and `Row` for headers/cells.
- Highlight the current cell if available.

---

## Step 3: Implement Strategy Guide Screen (`sharedUI/src/ui/screens/`)

### 3a: `StrategyGuideScreen`
- Tabbed interface (Hard, Soft, Pairs).
- Legend explaining colors.
- "Close" button or swipe-down to dismiss.

---

## Step 4: Integration (`sharedUI/src/ui/screens/BlackjackScreen.kt`)

### 4a: Add "Strategy" button
- Add a icon-only or small text button (e.g., a "lightbulb" icon) in the header or action row.
- Trigger the strategy guide display.

---

## Step 5: Localization (`sharedUI/composeResources/values/strings.xml`)

Add strings for actions, tab names, and UI labels.
```xml
<string name="strategy_guide">Strategy Guide</string>
<string name="tab_hard">Hard</string>
<string name="tab_soft">Soft</string>
<string name="tab_pairs">Pairs</string>
<string name="action_hit">H</string>
<string name="action_stand">S</string>
<string name="action_double">D</string>
<string name="action_split">P</string>
```

---

## Verification Plan

### Automated Tests
- Unit test `StrategyProvider` to ensure it returns correct actions for boundary cases (e.g., Hard 12 vs 2, Soft 18 vs 9).
- `./amper test -m core -p jvm`

### Manual Verification
- Open Strategy Guide.
- Switch between 3 tabs.
- Verify colors match the legend.
- Verify layout scales on portrait/landscape.
- (Optional) Verify current hand is highlighted when opened mid-game.
