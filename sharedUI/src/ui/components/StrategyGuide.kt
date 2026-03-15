package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.*
import io.github.smithjustinn.blackjack.ui.theme.*
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.*

@Composable
fun StrategyGuideOverlay(onDismiss: () -> Unit) {
    var selectedTab by remember { mutableStateOf<StrategyTab>(StrategyTab.Hard) }

    Surface(
        color = FeltDark.copy(alpha = 0.95f),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(Res.string.strategy_guide_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = PrimaryGold,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            StrategyTabs(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            StrategyChart(selectedTab)

            Spacer(modifier = Modifier.height(16.dp))

            StrategyLegend()
        }
    }
}

@Composable
private fun StrategyTabs(
    selectedTab: StrategyTab,
    onTabSelected: (StrategyTab) -> Unit
) {
    val tabs =
        listOf(
            StrategyTab.Hard to stringResource(Res.string.strategy_tab_hard),
            StrategyTab.Soft to stringResource(Res.string.strategy_tab_soft),
            StrategyTab.Pairs to stringResource(Res.string.strategy_tab_pairs)
        )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(GlassDark)
    ) {
        tabs.forEach { (tab, label) ->
            val isSelected = selectedTab == tab
            Box(
                modifier =
                    Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .background(if (isSelected) PrimaryGold else Color.Transparent)
                        .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) BackgroundDark else Color.White,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.StrategyChart(tab: StrategyTab) {
    val data =
        when (tab) {
            StrategyTab.Hard -> StrategyProvider.getHardStrategy()
            StrategyTab.Soft -> StrategyProvider.getSoftStrategy()
            StrategyTab.Pairs -> StrategyProvider.getPairsStrategy()
        }

    val dealerCards = listOf(2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        // Dealer Header
        Row(modifier = Modifier.fillMaxWidth().background(GlassLight)) {
            Box(modifier = Modifier.width(60.dp).padding(4.dp)) {
                Text(
                    text = "Hand",
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
            dealerCards.forEach { card ->
                Box(modifier = Modifier.weight(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (card == 11) "A" else card.toString(),
                        fontSize = 12.sp,
                        color = PrimaryGold,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(data) { cell ->
                Row(
                    modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.1f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Player Hand Label
                    Box(
                        modifier = Modifier.width(60.dp).padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = cell.playerValue,
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Strategy Actions
                    dealerCards.forEach { dealerCard ->
                        val action = cell.actions[dealerCard] ?: StrategyAction.HIT
                        StrategyActionCell(
                            action = action,
                            modifier = Modifier.weight(1f).aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrategyActionCell(
    action: StrategyAction,
    modifier: Modifier = Modifier
) {
    val color =
        when (action) {
            StrategyAction.HIT -> ChipGreen
            StrategyAction.STAND -> TacticalRed
            StrategyAction.DOUBLE -> PrimaryGold
            StrategyAction.SPLIT -> ChipPurple
        }

    val label =
        when (action) {
            StrategyAction.HIT -> stringResource(Res.string.strategy_action_hit)
            StrategyAction.STAND -> stringResource(Res.string.strategy_action_stand)
            StrategyAction.DOUBLE -> stringResource(Res.string.strategy_action_double)
            StrategyAction.SPLIT -> stringResource(Res.string.strategy_action_split)
        }

    Box(
        modifier =
            modifier
                .padding(1.dp)
                .background(color, RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            color = if (action == StrategyAction.DOUBLE) BackgroundDark else Color.White
        )
    }
}

@Composable
private fun StrategyLegend() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LegendItem(StrategyAction.HIT, "Hit")
        LegendItem(StrategyAction.STAND, "Stand")
        LegendItem(StrategyAction.DOUBLE, "Double")
        LegendItem(StrategyAction.SPLIT, "Split")
    }
}

@Composable
private fun LegendItem(
    action: StrategyAction,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StrategyActionCell(action = action, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 12.sp, color = Color.White)
    }
}
