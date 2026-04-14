package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.model.totalNetPayout
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.components.layout.ControlCenter
import io.github.smithjustinn.blackjack.ui.components.layout.Header
import io.github.smithjustinn.blackjack.ui.components.overlays.OverlayCardTable
import io.github.smithjustinn.blackjack.ui.effects.DealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold

/**
 * The root orchestrator for the Blackjack gameplay experience.
 *
 * This Composable serves as the **Layout Anchor** for the table. It manages the
 * spatial coordination between physical card positions (shoe, seats) and the
 * interactive HUD.
 *
 * **Functional Intent:**
 * - **Unidirectional Data Flow**: Consumes state from [component] and dispatches
 *   actions back to it.
 * - **Adaptive Layout**: Dynamically recalculates table overlays based on
 *   [BoxWithConstraints] dimensions to maintain aspect ratio integrity.
 * - **Animation Coupling**: Bridges the persistent [BlackjackScreenState] to
 *   transient [io.github.smithjustinn.blackjack.action.GameEffect]s via the
 *   internal `rememberBlackjackScreenState` hook.
 *
 * **Constraints:**
 * - Must be provided with a [BlackjackComponent] to drive its reactive lifecycle.
 * - Relies on [LocalDealAnimationRegistry] to coordinate global card movements.
 *
 * @param component The platform-agnostic state holder and action dispatcher.
 * @param modifier Root-level layout adjustments (e.g., padding, background).
 */
@Composable
fun BlackjackScreen(
    component: BlackjackComponent,
    modifier: Modifier = Modifier,
) {
    val screenState = rememberBlackjackScreenState(component)
    val insets = safeDrawingInsets()

    // ── Track positions of chips in the rack for fly-to-bet animations ────
    val rackChipOffsets = remember { mutableStateMapOf<Int, Offset>() }

    // Bolt Performance Optimization: Stabilize lambdas passed to GameOverlay to prevent
    // unnecessary recompositions when unrelated BlackjackScreenState properties change.
    val animState = screenState.animState
    val flashAlphaProvider = remember(animState) { { animState.flashAlpha.value } }
    val flashColorProvider = remember(animState) { { animState.flashColor } }
    val isPausedProvider = remember(animState) { { animState.isPaused } }
    val showBigWinBannerProvider = remember(animState) { { animState.showBigWinBanner } }
    val bigWinAmountProvider = remember(animState) { { animState.bigWinAmount } }

    BlackjackTheme {
        Box(modifier = modifier) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                TableSurface()

                // Enforce a portrait-like aspect ratio (9:16) if the window is too wide (letterboxing)
                val gameModifier =
                    if (maxHeight > maxWidth) {
                        Modifier.fillMaxSize()
                    } else {
                        val gameWidth = maxHeight * (9f / 16f)
                        Modifier.size(gameWidth, maxHeight).align(Alignment.Center)
                    }

                Box(
                    modifier =
                        gameModifier
                            .drawWithCache {
                                // 5. Active Hand Highlight Brushes — cached only on size change.
                                // Bolt Performance Optimization: Prevent 2x Brush.radialGradient allocations per frame
                                // during the active hand highlight spring animation.
                                val highlightRadius = size.maxDimension * 0.35f
                                val innerBrush =
                                    Brush.radialGradient(
                                        colors = listOf(PrimaryGold.copy(alpha = 0.15f), Color.Transparent),
                                        center = Offset.Zero,
                                        radius = highlightRadius * 0.4f
                                    )
                                val outerBrush =
                                    Brush.radialGradient(
                                        colors = listOf(PrimaryGold.copy(alpha = 0.08f), Color.Transparent),
                                        center = Offset.Zero,
                                        radius = highlightRadius
                                    )

                                onDrawBehind {
                                    val highlightPos = screenState.activeHandHighlightPositionState.value
                                    if (highlightPos != Offset.Zero) {
                                        // Use translate to reuse the pre-computed brushes with Offset.Zero center
                                        translate(highlightPos.x, highlightPos.y) {
                                            drawCircle(
                                                brush = innerBrush,
                                                radius = highlightRadius * 0.4f,
                                                center = Offset.Zero
                                            )
                                            drawCircle(
                                                brush = outerBrush,
                                                radius = highlightRadius,
                                                center = Offset.Zero
                                            )
                                        }
                                    }
                                }
                            }.graphicsLayer {
                                val densityVal = density
                                translationX = screenState.animState.shakeOffset.value * densityVal
                            },
                ) {
                    CompositionLocalProvider(LocalDealAnimationRegistry provides screenState.dealRegistry) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .windowInsetsPadding(
                                        insets.only(WindowInsetsSides.Horizontal)
                                    ),
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned {
                                            screenState.onHeaderPositioned(
                                                it.positionInRoot() + Offset(80.dp.value, 40.dp.value)
                                            )
                                        }
                            ) {
                                Header(
                                    balance = screenState.state.balance,
                                    isAutoDealEnabled = screenState.appSettings.isAutoDealEnabled,
                                    onAutoDealToggle = screenState.onAutoDealToggle,
                                    onSettingsClick = screenState.onSettingsClick,
                                    onStrategyClick = screenState.onStrategyClick,
                                    onRulesClick = screenState.onRulesClick
                                )
                            }

                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .onGloballyPositioned { coords ->
                                            screenState.dealRegistry.gameplayAreaOffset =
                                                coords.positionInRoot()
                                            screenState.dealRegistry.gameplayAreaSize = coords.size
                                        },
                            ) {
                                BlackjackLayout(
                                    dealRegistry = screenState.dealRegistry,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            ControlCenter(
                                isBetting = screenState.state.isBettingPhase,
                                isPlaying = screenState.state.isPlayingPhase,
                                totalBet = screenState.state.totalBet,
                                balance = screenState.state.balance,
                                canDeal = screenState.state.canDeal,
                                canResetBet = screenState.state.canResetBet,
                                canSplit = screenState.state.canSplit,
                                canDoubleDown = screenState.state.canDoubleDown,
                                canSurrender = screenState.state.canSurrender,
                                activeHandTension =
                                    screenState.state.playerHands
                                        .getOrNull(screenState.state.activeHandIndex)
                                        ?.tension
                                        ?: 0.0f,
                                onHit = screenState.onHit,
                                onStand = screenState.onStand,
                                onDoubleDown = screenState.onDoubleDown,
                                onSplit = screenState.onSplit,
                                onSurrender = screenState.onSurrender,
                                selectedAmount = screenState.selectedAmount,
                                onChipSelected = screenState.onChipSelected,
                                onResetBet = screenState.onResetBet,
                                onDeal = screenState.onDeal,
                                isCompact = screenState.isMultiHand,
                                onChipPositioned = { value, offset -> rackChipOffsets[value] = offset },
                            )
                        }

                        // Overlay layer (full-bleed within the game bounds)
                        Box(
                            modifier =
                                Modifier.fillMaxSize().onGloballyPositioned { coords ->
                                    screenState.dealRegistry.overlayOffset = coords.positionInRoot()
                                }
                        ) {
                            // Cards + HUD rendered in overlay (below other overlays)
                            OverlayCardTable(
                                state = screenState.state,
                                nearMissHandIndex = screenState.animState.nearMissHandIndex,
                                modifier = Modifier.zIndex(1f),
                            )

                            // Game Overlays & Status
                            GameOverlay(
                                status = screenState.state.status,
                                sideBetResults = screenState.state.sideBetResults,
                                isBlackjack = screenState.state.hasPlayerBlackjackWin,
                                isBust = screenState.state.hasPlayerBustLoss,
                                netPayout = screenState.state.totalNetPayout,
                                onTakeInsurance = screenState.onTakeInsurance,
                                onDeclineInsurance = screenState.onDeclineInsurance,
                                flashAlphaProvider = flashAlphaProvider,
                                flashColorProvider = flashColorProvider,
                                showInsuranceOverlay = screenState.showInsuranceOverlay,
                                showConfetti = screenState.showConfetti,
                                showSparkle = screenState.showSparkle,
                                isPaused = isPausedProvider,
                                showBigWinBanner = showBigWinBannerProvider,
                                bigWinAmount = bigWinAmountProvider,
                                modifier = Modifier.zIndex(2f),
                            )
                            BettingLayer(
                                isBetting = screenState.state.isBettingPhase,
                                handCount = screenState.state.handCount,
                                sideBets = screenState.state.sideBets,
                                playerHands = screenState.state.playerHands,
                                animState = screenState.animState,
                                component = component,
                                selectedAmount = screenState.selectedAmount,
                                rackChipOffsets = rackChipOffsets,
                            )

                            OverlayLayer(
                                showSettings = screenState.showSettings,
                                showRules = screenState.showRules,
                                showStrategy = screenState.showStrategy,
                                appSettings = screenState.appSettings,
                                component = component,
                                onDismissSettings = screenState.onDismissSettings,
                                onDismissRules = screenState.onDismissRules,
                                onDismissStrategy = screenState.onDismissStrategy,
                            )
                        }
                    } // CompositionLocalProvider
                }
            } // End of outer BoxWithConstraints
        } // End of BlackjackTheme
    }
}

@Composable
private fun BlackjackLayout(
    dealRegistry: DealAnimationRegistry,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Shoe Position Anchor in top-right corner
        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(80.dp, 110.dp)
                    .onGloballyPositioned { coords ->
                        val pos = coords.positionInRoot()
                        dealRegistry.shoePosition =
                            Offset(
                                pos.x + coords.size.width / 2f,
                                pos.y + coords.size.height / 2f,
                            )
                    }
        )
    }
}
