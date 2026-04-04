package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.ui.safeDrawingInsets
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import io.github.smithjustinn.blackjack.utils.formatWithCommas
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.balance
import sharedui.generated.resources.btn_auto_deal_description
import sharedui.generated.resources.btn_rules_description
import sharedui.generated.resources.btn_settings_description
import sharedui.generated.resources.btn_strategy_description
import sharedui.generated.resources.currency_symbol
import sharedui.generated.resources.currency_template
import sharedui.generated.resources.emoji_bulb
import sharedui.generated.resources.emoji_clock
import sharedui.generated.resources.emoji_gear
import sharedui.generated.resources.emoji_lightning
import sharedui.generated.resources.emoji_scroll
import sharedui.generated.resources.financial_data_content_description
import sharedui.generated.resources.header_bet_range

@Composable
fun Header(
    balance: Int,
    isAutoDealEnabled: Boolean,
    onAutoDealToggle: () -> Unit,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onStrategyClick: () -> Unit = {},
    onRulesClick: () -> Unit = {}
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(GlassDark)
                .windowInsetsPadding(safeDrawingInsets().only(WindowInsetsSides.Top))
                .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left Side: AutoDeal & Table Info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AutoDealIcon(enabled = isAutoDealEnabled, onClick = onAutoDealToggle)
            TableInfoBadge(minBet = 10, maxBet = 500)
        }

        // Center: Casino Vault (Balance)
        CasinoVault(balance = balance)

        // Right Side: Control Icons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeaderIcon(
                "rules",
                contentDescription = stringResource(Res.string.btn_rules_description),
                onClick = onRulesClick
            )
            HeaderIcon(
                "strategy",
                contentDescription = stringResource(Res.string.btn_strategy_description),
                onClick = onStrategyClick
            )
            HeaderIcon(
                "settings",
                contentDescription = stringResource(Res.string.btn_settings_description),
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun TableInfoBadge(
    minBet: Int,
    maxBet: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.3f))
                .border(0.5.dp, PrimaryGold.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(Res.string.header_bet_range, minBet, maxBet),
            style = MaterialTheme.typography.labelSmall,
            color = PrimaryGold.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun CasinoVault(
    balance: Int,
    modifier: Modifier = Modifier
) {
    val animatedBalance by animateIntAsState(
        targetValue = balance,
        animationSpec = tween(durationMillis = 800)
    )
    val balanceLabel = stringResource(Res.string.balance)
    val formattedBalance = animatedBalance.formatWithCommas()
    val fullDescription =
        stringResource(
            Res.string.financial_data_content_description,
            balanceLabel,
            stringResource(Res.string.currency_template, formattedBalance)
        )

    Box(
        modifier =
            modifier
                .shadow(8.dp, RoundedCornerShape(percent = 50), spotColor = PrimaryGold.copy(alpha = 0.4f))
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = fullDescription
                }.background(
                    brush =
                        Brush.verticalGradient(
                            0.0f to Color(0xFF2C2C2C),
                            0.5f to Color.Black,
                            1.0f to Color(0xFF1A1A1A)
                        ),
                    shape = RoundedCornerShape(percent = 50)
                ).border(
                    width = 1.5.dp,
                    brush =
                        Brush.linearGradient(
                            0.0f to PrimaryGold,
                            0.5f to PrimaryGold.copy(alpha = 0.3f),
                            1.0f to PrimaryGold
                        ),
                    shape = RoundedCornerShape(percent = 50)
                ).padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(Res.string.currency_symbol),
                color = PrimaryGold,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp
            )
            Text(
                text = formattedBalance,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
internal fun AutoDealIcon(
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val autoDealDescription = stringResource(Res.string.btn_auto_deal_description)
    val infiniteTransition = rememberInfiniteTransition(label = "autoDealPulse")

    // Breathing scale: 1.0 → 1.1 → 1.0
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 1.1f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(AnimationConstants.GlowBreatheDuration, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "breathScale",
    )

    // Border alpha: wider range than before (0.3 → 1.0)
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (enabled) 1.0f else 0.3f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(AnimationConstants.AutoDealBorderPulseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "autoBorderAlpha",
    )

    // Sonar pulse rings
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 2.0f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(AnimationConstants.AutoDealSonarPulseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseScale1",
    )
    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(AnimationConstants.AutoDealSonarPulseDuration, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseAlpha1",
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (enabled) 2.0f else 1.0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        AnimationConstants.AutoDealSonarPulseDuration,
                        delayMillis = AnimationConstants.AutoDealSonarPulseDelay,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseScale2",
    )
    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec =
            infiniteRepeatable(
                animation =
                    tween(
                        AnimationConstants.AutoDealSonarPulseDuration,
                        delayMillis = AnimationConstants.AutoDealSonarPulseDelay,
                        easing = LinearEasing
                    ),
                repeatMode = RepeatMode.Restart,
            ),
        label = "pulseAlpha2",
    )

    val backgroundColor = GlassDark

    Box(
        modifier =
            modifier
                .size(32.dp)
                .semantics { contentDescription = autoDealDescription }
                .drawBehind {
                    if (enabled) {
                        drawCircle(
                            color = PrimaryGold.copy(alpha = pulseAlpha1),
                            radius = (size.minDimension / 2) * pulseScale1,
                        )
                        drawCircle(
                            color = PrimaryGold.copy(alpha = pulseAlpha2),
                            radius = (size.minDimension / 2) * pulseScale2,
                        )
                    }
                }.graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.background(backgroundColor, RoundedCornerShape(16.dp))
                .drawWithCache {
                    val strokeWidth = 1.dp.toPx()
                    val halfStroke = strokeWidth / 2f
                    val stroke = Stroke(strokeWidth)
                    val topLeftOffset = Offset(halfStroke, halfStroke)
                    val strokeSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val cornerRadius = CornerRadius(16.dp.toPx() - halfStroke)

                    onDrawWithContent {
                        drawContent()
                        // Bolt Performance Optimization:
                        // Deferring `borderAlpha` state read to the draw phase eliminates O(Frames)
                        // recompositions. Hoisting `Stroke` out of the draw loop eliminates GC overhead.
                        val currentBorderColor = if (enabled) PrimaryGold.copy(alpha = borderAlpha) else GlassLight
                        drawRoundRect(
                            color = currentBorderColor,
                            topLeft = topLeftOffset,
                            size = strokeSize,
                            style = stroke,
                            cornerRadius = cornerRadius
                        )
                    }
                }.clip(RoundedCornerShape(16.dp))
                .toggleable(
                    value = enabled,
                    role = Role.Switch,
                    onValueChange = { onClick() }
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(Res.string.emoji_lightning), fontSize = 14.sp)
    }
}

@Composable
private fun HeaderIcon(
    text: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .semantics { this.contentDescription = contentDescription }
                .background(GlassDark, RoundedCornerShape(16.dp))
                .border(1.dp, PrimaryGold.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable(role = Role.Button) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text =
                when (text) {
                    "settings" -> stringResource(Res.string.emoji_gear)
                    "strategy" -> stringResource(Res.string.emoji_bulb)
                    "rules" -> stringResource(Res.string.emoji_scroll)
                    else -> stringResource(Res.string.emoji_clock)
                },
            fontSize = 14.sp,
            modifier = Modifier.alpha(0.9f)
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun HeaderPreview() {
    BlackjackTheme {
        Header(
            balance = 1000,
            isAutoDealEnabled = false,
            onAutoDealToggle = {},
            onSettingsClick = {},
            onStrategyClick = {},
            onRulesClick = {}
        )
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun HeaderAutoDealPreview() {
    BlackjackTheme {
        Header(
            balance = 2500,
            isAutoDealEnabled = true,
            onAutoDealToggle = {},
            onSettingsClick = {},
            onStrategyClick = {},
            onRulesClick = {}
        )
    }
}
