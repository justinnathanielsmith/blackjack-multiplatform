package io.github.smithjustinn.blackjack.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.github.smithjustinn.blackjack.data.AppSettings
import io.github.smithjustinn.blackjack.presentation.BlackjackComponent
import io.github.smithjustinn.blackjack.ui.components.RulesOverlay
import io.github.smithjustinn.blackjack.ui.components.SettingsOverlay
import io.github.smithjustinn.blackjack.ui.components.StrategyGuideOverlay
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants

@Composable
fun OverlayLayer(
    showSettings: Boolean,
    showRules: Boolean,
    showStrategy: Boolean,
    appSettings: AppSettings,
    component: BlackjackComponent,
    onDismissSettings: () -> Unit,
    onDismissRules: () -> Unit,
    onDismissStrategy: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = showSettings,
        modifier = modifier.zIndex(10f),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
    ) {
        SettingsOverlay(
            settings = appSettings,
            onUpdateSettings = component::updateSettings,
            onResetBalance = component::resetBalance,
            onDismiss = onDismissSettings,
            modifier = modifier
        )
    }

    AnimatedVisibility(
        visible = showRules,
        modifier = modifier.zIndex(10f),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
    ) {
        RulesOverlay(
            onDismiss = onDismissRules,
            modifier = modifier
        )
    }

    AnimatedVisibility(
        visible = showStrategy,
        modifier = modifier.zIndex(10f),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(tween(AnimationConstants.OverlayEnterDuration)),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(tween(AnimationConstants.OverlayExitDuration)),
    ) {
        StrategyGuideOverlay(
            onDismiss = onDismissStrategy,
            modifier = modifier
        )
    }
}
