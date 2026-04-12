package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.model.BlackjackConfig
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationOrchestrator
import io.github.smithjustinn.blackjack.ui.animation.BlackjackAnimationState
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry

data class BlackjackScreenState(
    val state: GameState,
    val appSettings: AppSettings,
    val animState: BlackjackAnimationState,
    val dealRegistry: DealAnimationRegistry,
    val selectedAmount: Int,
    val headerBalanceOffset: Offset,
    val showSettings: Boolean,
    val showStrategy: Boolean,
    val showRules: Boolean,
    val isTerminal: Boolean,
    val isMultiHand: Boolean,
    // Presentation mapping: domain status → overlay visibility flags (no GameStatus checks in Composables)
    val showInsuranceOverlay: Boolean,
    val showConfetti: Boolean,
    val showSparkle: Boolean,
    val activeHandHighlightPositionState: State<Offset>,
    val onResetBet: () -> Unit,
    val onDeal: () -> Unit,
    val onChipSelected: (Int) -> Unit,
    val onAutoDealToggle: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onStrategyClick: () -> Unit,
    val onRulesClick: () -> Unit,
    val onDismissSettings: () -> Unit,
    val onDismissRules: () -> Unit,
    val onDismissStrategy: () -> Unit,
    val onHeaderPositioned: (Offset) -> Unit,
)

private data class BlackjackCallbacks(
    val onResetBet: () -> Unit,
    val onDeal: () -> Unit,
    val onChipSelected: (Int) -> Unit,
    val onAutoDealToggle: () -> Unit,
    val onSettingsClick: () -> Unit,
    val onStrategyClick: () -> Unit,
    val onRulesClick: () -> Unit,
    val onDismissSettings: () -> Unit,
    val onDismissRules: () -> Unit,
    val onDismissStrategy: () -> Unit,
    val onHeaderPositioned: (Offset) -> Unit,
)

@Composable
private fun rememberCallbacks(
    component: BlackjackComponent,
    selectedAmountSetter: (Int) -> Unit,
    showSettingsSetter: (Boolean) -> Unit,
    showStrategySetter: (Boolean) -> Unit,
    showRulesSetter: (Boolean) -> Unit,
    headerOffsetSetter: (Offset) -> Unit,
): BlackjackCallbacks {
    // Composite reset delegated to component — UI layer no longer encodes which actions pair together.
    val onResetBet = remember(component) { { component.onResetBets() } }
    val onDeal =
        remember(component) {
            {
                component.onPlayDeal()
                component.onAction(GameAction.Deal)
            }
        }
    val onChipSelected =
        remember(component) {
            { amount: Int ->
                selectedAmountSetter(amount)
                component.onPlayPlink(amount)
            }
        }
    val onAutoDealToggle =
        remember(component) {
            { component.updateSettings { it.copy(isAutoDealEnabled = !it.isAutoDealEnabled) } }
        }
    val onSettingsClick = remember { { showSettingsSetter(true) } }
    val onStrategyClick = remember { { showStrategySetter(true) } }
    val onRulesClick = remember { { showRulesSetter(true) } }
    val onDismissSettings = remember { { showSettingsSetter(false) } }
    val onDismissRules = remember { { showRulesSetter(false) } }
    val onDismissStrategy = remember { { showStrategySetter(false) } }
    return BlackjackCallbacks(
        onResetBet = onResetBet,
        onDeal = onDeal,
        onChipSelected = onChipSelected,
        onAutoDealToggle = onAutoDealToggle,
        onSettingsClick = onSettingsClick,
        onStrategyClick = onStrategyClick,
        onRulesClick = onRulesClick,
        onDismissSettings = onDismissSettings,
        onDismissRules = onDismissRules,
        onDismissStrategy = onDismissStrategy,
        onHeaderPositioned = { headerOffsetSetter(it) },
    )
}

@Composable
fun rememberBlackjackScreenState(component: BlackjackComponent): BlackjackScreenState {
    val state by component.state.collectAsState()
    val appSettings by component.appSettings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }
    var showStrategy by remember { mutableStateOf(false) }
    var showRules by remember { mutableStateOf(false) }
    val animState = remember { BlackjackAnimationState() }
    var headerBalanceOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAmount by remember { mutableStateOf(BlackjackConfig.DEFAULT_CHIP_AMOUNT) }

    // ── Callbacks ──
    val callbacks =
        rememberCallbacks(
            component = component,
            selectedAmountSetter = { selectedAmount = it },
            showSettingsSetter = { showSettings = it },
            showStrategySetter = { showStrategy = it },
            showRulesSetter = { showRules = it },
            headerOffsetSetter = { headerBalanceOffset = it },
        )

    // ── Derived State ──
    val isTerminal by remember { derivedStateOf { state.status.isTerminal() } }
    val isMultiHand by remember { derivedStateOf { state.playerHands.size > 1 } }
    // Presentation mapping: domain status → overlay visibility flags (no GameStatus checks in Composables)
    val showInsuranceOverlay by remember { derivedStateOf { state.status == GameStatus.INSURANCE_OFFERED } }
    val showConfetti by remember { derivedStateOf { state.status == GameStatus.PLAYER_WON } }
    val showSparkle by remember {
        derivedStateOf { state.status == GameStatus.PLAYER_WON && state.hasPlayerBlackjackWin }
    }

    // ── Animation Setup ──
    val dealRegistry = remember { DealAnimationRegistry() }

    LaunchedEffect(state.status) {
        if (state.status == GameStatus.BETTING) {
            dealRegistry.tableLayout = null
        }
    }

    // handZones[0] = dealer, handZones[1..N] = player hands
    // Bolt Performance Optimization: Use derivedStateOf to narrow the scope of state reads
    // for the animation target. This prevents animateOffsetAsState from re-triggering
    // its internal logic when unrelated GameState properties (like balance) change.
    val activeHandHighlightTarget by remember {
        derivedStateOf {
            if (state.status == GameStatus.PLAYING) {
                val zone = dealRegistry.tableLayout?.handZones?.getOrNull(state.activeHandIndex + 1)
                if (zone != null) zone.clusterCenter + dealRegistry.gameplayAreaOffset else Offset.Zero
            } else {
                Offset.Zero
            }
        }
    }

    val activeHandHighlightPositionState =
        animateOffsetAsState(
            targetValue = activeHandHighlightTarget,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "activeHandHighlight",
        )

    // Animation orchestration: effects pipeline + state-driven flash/shake/payouts
    LaunchedEffect(component) {
        BlackjackAnimationOrchestrator.orchestrate(
            effects = component.effects,
            stateFlow = component.state,
            animState = animState,
            audioService = component.audioService,
            hapticsService = component.hapticsService,
            dealRegistry = dealRegistry,
        )
    }

    return BlackjackScreenState(
        state = state,
        appSettings = appSettings,
        animState = animState,
        dealRegistry = dealRegistry,
        selectedAmount = selectedAmount,
        headerBalanceOffset = headerBalanceOffset,
        showSettings = showSettings,
        showStrategy = showStrategy,
        showRules = showRules,
        isTerminal = isTerminal,
        isMultiHand = isMultiHand,
        showInsuranceOverlay = showInsuranceOverlay,
        showConfetti = showConfetti,
        showSparkle = showSparkle,
        activeHandHighlightPositionState = activeHandHighlightPositionState,
        onResetBet = callbacks.onResetBet,
        onDeal = callbacks.onDeal,
        onChipSelected = callbacks.onChipSelected,
        onAutoDealToggle = callbacks.onAutoDealToggle,
        onSettingsClick = callbacks.onSettingsClick,
        onStrategyClick = callbacks.onStrategyClick,
        onRulesClick = callbacks.onRulesClick,
        onDismissSettings = callbacks.onDismissSettings,
        onDismissRules = callbacks.onDismissRules,
        onDismissStrategy = callbacks.onDismissStrategy,
        onHeaderPositioned = callbacks.onHeaderPositioned,
    )
}
