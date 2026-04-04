package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.SideBetResult
import io.github.smithjustinn.blackjack.SideBetType
import io.github.smithjustinn.blackjack.isTerminal
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.components.GameStatusMessage
import io.github.smithjustinn.blackjack.ui.components.GameStatusToast
import io.github.smithjustinn.blackjack.ui.components.InsuranceOverlay
import io.github.smithjustinn.blackjack.ui.effects.BigWinBanner
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.effects.SparkleEffect
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.side_bet_colored_pair
import sharedui.generated.resources.side_bet_flush
import sharedui.generated.resources.side_bet_mixed_pair
import sharedui.generated.resources.side_bet_perfect_pair
import sharedui.generated.resources.side_bet_result_template
import sharedui.generated.resources.side_bet_straight
import sharedui.generated.resources.side_bet_straight_flush
import sharedui.generated.resources.side_bet_suited_triple
import sharedui.generated.resources.side_bet_three_of_a_kind

@Composable
fun GameOverlay(
    sideBetResults: Map<SideBetType, SideBetResult>,
    status: GameStatus,
    playerHands: List<Hand>,
    netPayout: Int?,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
    flashColorProvider: () -> Color,
    isPaused: () -> Boolean = { false },
    showBigWinBanner: () -> Boolean = { false },
    bigWinAmount: () -> Int = { 0 },
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        SideBetResultsOverlay(sideBetResults = sideBetResults)

        BlackjackGameOverlay(
            status = status,
            playerHands = playerHands,
            netPayout = netPayout,
            component = component,
            flashAlphaProvider = flashAlphaProvider,
            flashColorProvider = flashColorProvider,
            isPaused = isPaused,
            showBigWinBanner = showBigWinBanner,
            bigWinAmount = bigWinAmount,
            modifier = Modifier.zIndex(5f),
        )
    }
}

@Composable
private fun BlackjackGameOverlay(
    status: GameStatus,
    playerHands: List<Hand>,
    netPayout: Int?,
    component: BlackjackComponent,
    flashAlphaProvider: () -> Float,
    flashColorProvider: () -> Color,
    isPaused: () -> Boolean = { false },
    showBigWinBanner: () -> Boolean = { false },
    bigWinAmount: () -> Int = { 0 },
    modifier: Modifier = Modifier,
) {
    val isBlackjack by remember(status, playerHands) {
        derivedStateOf {
            status == GameStatus.PLAYER_WON && playerHands.any { it.isBlackjack }
        }
    }
    // isProcessState and isTerminalState are mutually exclusive by domain definition
    val isProcessState by remember(status) {
        derivedStateOf { status == GameStatus.DEALING || status == GameStatus.DEALER_TURN }
    }
    val isTerminalState by remember(status) { derivedStateOf { status.isTerminal() } }

    val scrimAlpha by animateFloatAsState(
        targetValue = if (isTerminalState) 0.62f else 0f,
        animationSpec =
            tween(
                durationMillis = AnimationConstants.StatusMessageEnterDuration,
                easing = FastOutSlowInEasing
            ),
        label = "terminalScrimAlpha",
    )

    val onTakeInsurance = remember(component) { { component.onAction(GameAction.TakeInsurance) } }
    val onDeclineInsurance = remember(component) { { component.onAction(GameAction.DeclineInsurance) } }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        // Dark scrim fades in on terminal states, dims table so result panel is always readable
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        if (scrimAlpha > 0f) drawRect(Color.Black.copy(alpha = scrimAlpha))
                    },
        )

        // Tier 1 — Process states: compact toast slides in from top
        AnimatedVisibility(
            visible = isProcessState,
            enter =
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium),
                ) + fadeIn(animationSpec = tween(AnimationConstants.StatusMessageEnterDuration)),
            exit =
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(AnimationConstants.StatusMessageExitDuration),
                ) + fadeOut(animationSpec = tween(AnimationConstants.StatusMessageExitDuration)),
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        ) {
            GameStatusToast(status = status)
        }

        // Tier 2 — Terminal states: dramatic centered panel
        AnimatedVisibility(
            visible = isTerminalState,
            enter =
                fadeIn(animationSpec = tween(AnimationConstants.StatusMessageEnterDuration)) +
                    scaleIn(initialScale = 0.5f, animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f)),
            exit =
                fadeOut(animationSpec = tween(AnimationConstants.StatusMessageExitDuration)) +
                    scaleOut(targetScale = 0.8f),
        ) {
            GameStatusMessage(status = status, netPayout = netPayout, isBlackjack = isBlackjack)
        }

        if (status == GameStatus.INSURANCE_OFFERED) {
            InsuranceOverlay(
                onInsure = onTakeInsurance,
                onDecline = onDeclineInsurance,
            )
        }

        if (status == GameStatus.PLAYER_WON) {
            ConfettiEffect(
                particleCount = if (isBlackjack) 250 else 120,
                isBlackjack = isBlackjack,
                isPaused = isPaused,
            )
        }

        if (status == GameStatus.PLAYER_WON && isBlackjack) {
            SparkleEffect(isPaused = isPaused)
        }

        BigWinBanner(
            visible = showBigWinBanner(),
            amount = bigWinAmount(),
        )

        // Bolt Performance Optimization: Defer state read to draw phase to prevent O(Frames) recompositions
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val alpha = flashAlphaProvider()
                        if (alpha > 0f) {
                            drawRect(flashColorProvider().copy(alpha = alpha))
                        }
                    },
        )
    }
}

@Composable
private fun SideBetResultsOverlay(sideBetResults: Map<SideBetType, SideBetResult>) {
    AnimatedVisibility(
        visible = sideBetResults.isNotEmpty(),
        enter =
            slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.StatusMessageEnterDuration)),
        exit =
            slideOutVertically(
                targetOffsetY = { it }
            ) + fadeOut(tween(AnimationConstants.StatusMessageExitDuration)),
        modifier =
            Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp)
                .zIndex(4f),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                sideBetResults.forEach { (_, result) ->
                    Box(
                        modifier =
                            Modifier
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .border(1.dp, PrimaryGold.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text =
                                stringResource(
                                    Res.string.side_bet_result_template,
                                    getLocalizedOutcomeName(result.outcomeName),
                                    result.payoutAmount,
                                ),
                            color = PrimaryGold,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun getLocalizedOutcomeName(name: String): String {
    return when (name) {
        "Perfect Pair" -> stringResource(Res.string.side_bet_perfect_pair)
        "Colored Pair" -> stringResource(Res.string.side_bet_colored_pair)
        "Mixed Pair" -> stringResource(Res.string.side_bet_mixed_pair)
        "Suited Triple" -> stringResource(Res.string.side_bet_suited_triple)
        "Straight Flush" -> stringResource(Res.string.side_bet_straight_flush)
        "Three of a Kind" -> stringResource(Res.string.side_bet_three_of_a_kind)
        "Straight" -> stringResource(Res.string.side_bet_straight)
        "Flush" -> stringResource(Res.string.side_bet_flush)
        else -> name
    }
}
