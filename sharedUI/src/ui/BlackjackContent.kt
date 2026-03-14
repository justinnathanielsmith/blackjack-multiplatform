package io.github.smithjustinn.blackjack.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.smithjustinn.blackjack.GameAction
import io.github.smithjustinn.blackjack.GameEffect
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.GameStatus
import io.github.smithjustinn.blackjack.Hand
import io.github.smithjustinn.blackjack.di.LocalAppGraph
import io.github.smithjustinn.blackjack.services.AudioService
import io.github.smithjustinn.blackjack.ui.components.CasinoButton
import io.github.smithjustinn.blackjack.ui.effects.ConfettiEffect
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.BackgroundDark
import io.github.smithjustinn.blackjack.ui.theme.FeltDark
import io.github.smithjustinn.blackjack.ui.theme.FeltGreen
import io.github.smithjustinn.blackjack.ui.theme.GlassDark
import io.github.smithjustinn.blackjack.ui.theme.GlassLight
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.deal
import sharedui.generated.resources.hit
import sharedui.generated.resources.split
import sharedui.generated.resources.stand
import sharedui.generated.resources.status_betting
import sharedui.generated.resources.status_dealer_turn
import sharedui.generated.resources.status_dealer_won
import sharedui.generated.resources.status_idle
import sharedui.generated.resources.status_player_won
import sharedui.generated.resources.status_playing
import sharedui.generated.resources.status_push

@Composable
fun BlackjackContent(component: BlackjackComponent) {
    val state by component.state.collectAsState()
    val audioService = LocalAppGraph.current.audioService
    val hapticsService = LocalAppGraph.current.hapticsService
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(component) {
        component.effects.collect { effect: GameEffect ->
            handleGameEffect(effect, hapticsService)
        }
    }

    LaunchedEffect(state.status) {
        when (state.status) {
            GameStatus.PLAYER_WON -> {
                audioService.playEffect(AudioService.SoundEffect.WIN)
            }
            GameStatus.DEALER_WON -> {
                audioService.playEffect(AudioService.SoundEffect.LOSE)
                launch {
                    shakeOffset.animateTo(15f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(-15f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(10f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(-10f, spring<Float>(stiffness = Spring.StiffnessHigh))
                    shakeOffset.animateTo(0f, spring<Float>(stiffness = Spring.StiffnessMedium))
                }
            }
            GameStatus.PUSH -> audioService.playEffect(AudioService.SoundEffect.PUSH)
            else -> {}
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
        label = "pulseScale"
    )

    BlackjackTheme {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            0.0f to FeltGreen,
                            1.0f to FeltDark,
                            radius = 2000f
                        )
                    ).offset(x = shakeOffset.value.dp)
        ) {
            val isLandscape = maxWidth > maxHeight
            val isCompactHeight = maxHeight < 500.dp
            val useCompactUI = isLandscape && isCompactHeight

            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
            ) {
                Header(balance = state.balance)

                Box(modifier = Modifier.weight(1f)) {
                    if (state.status == GameStatus.PLAYER_WON) {
                        ConfettiEffect()
                    }

                    if (state.status == GameStatus.BETTING) {
                        BettingPhaseContent(
                            state = state,
                            component = component,
                            audioService = audioService,
                            isCompact = useCompactUI,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (useCompactUI) {
                        LandscapeLayout(
                            state = state,
                            audioService = audioService,
                            component = component,
                            pulseScale = pulseScale
                        )
                    } else {
                        PortraitLayout(
                            state = state,
                            audioService = audioService,
                            component = component,
                            pulseScale = pulseScale
                        )
                    }

                    if (state.status == GameStatus.INSURANCE_OFFERED) {
                        InsuranceOverlay(
                            onInsure = { component.onAction(GameAction.TakeInsurance) },
                            onDecline = { component.onAction(GameAction.DeclineInsurance) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InsuranceOverlay(
    onInsure: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(GlassDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier =
                Modifier
                    .padding(32.dp)
                    .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(24.dp))
                    .border(1.dp, GlassLight, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "INSURANCE?",
                style = MaterialTheme.typography.headlineMedium,
                color = PrimaryGold,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Dealer shows an ACE. Insurance pays 2:1.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                CasinoButton(
                    text = "NO THANKS",
                    onClick = onDecline,
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                )
                CasinoButton(
                    text = "INSURE",
                    onClick = onInsure,
                    modifier = Modifier.weight(1f),
                    isStrategic = true
                )
            }
        }
    }
}

@Composable
private fun PortraitLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Dealer Hand
        val dealerDisplayScore = if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
        HandContainer(title = "Dealer", score = dealerDisplayScore) {
            HandRow(state.dealerHand)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (state.status != GameStatus.PLAYING &&
            state.status != GameStatus.BETTING &&
            state.status != GameStatus.INSURANCE_OFFERED
        ) {
            GameStatusMessage(status = state.status, pulseScale = pulseScale, isCompact = false)
        }

        Spacer(modifier = Modifier.weight(1f))

        val splitHand = state.splitHand
        if (splitHand != null) {
            val primaryActive = !state.isPlayingSplitHand && state.status == GameStatus.PLAYING
            val splitActive = state.isPlayingSplitHand && state.status == GameStatus.PLAYING

            HandContainer(
                title = "Hand 1",
                score = state.playerHand.score,
                bet = state.currentBet,
                isActive = primaryActive,
                isPending = !primaryActive && state.status == GameStatus.PLAYING
            ) {
                HandRow(state.playerHand)
            }

            Spacer(modifier = Modifier.height(16.dp))

            HandContainer(
                title = "Hand 2",
                score = splitHand.score,
                bet = state.splitBet,
                isActive = splitActive,
                isPending = !splitActive && state.status == GameStatus.PLAYING
            ) {
                HandRow(splitHand)
            }
        } else {
            HandContainer(
                title = "You",
                score = state.playerHand.score,
                bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                isActive = state.status == GameStatus.PLAYING
            ) {
                HandRow(state.playerHand)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        GameActions(
            state = state,
            audioService = audioService,
            component = component,
            isCompact = false
        )
    }
}

@Composable
private fun LandscapeLayout(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    pulseScale: Float
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Left side: Cards
        Column(
            modifier = Modifier.weight(1.2f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val dealerDisplayScore = if (state.status == GameStatus.PLAYING) state.dealerHand.visibleScore else state.dealerHand.score
            HandContainer(title = "Dealer", score = dealerDisplayScore) {
                HandRow(state.dealerHand)
            }

            Spacer(modifier = Modifier.height(8.dp))

            val splitHand = state.splitHand
            if (splitHand != null) {
                val primaryActive = !state.isPlayingSplitHand && state.status == GameStatus.PLAYING
                val splitActive = state.isPlayingSplitHand && state.status == GameStatus.PLAYING

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HandContainer(
                        title = "H1",
                        score = state.playerHand.score,
                        bet = state.currentBet,
                        isActive = primaryActive,
                        isPending = !primaryActive && state.status == GameStatus.PLAYING,
                        modifier = Modifier.weight(1f)
                    ) {
                        HandRow(state.playerHand)
                    }

                    HandContainer(
                        title = "H2",
                        score = splitHand.score,
                        bet = state.splitBet,
                        isActive = splitActive,
                        isPending = !splitActive && state.status == GameStatus.PLAYING,
                        modifier = Modifier.weight(1f)
                    ) {
                        HandRow(splitHand)
                    }
                }
            } else {
                HandContainer(
                    title = "You",
                    score = state.playerHand.score,
                    bet = if (state.status != GameStatus.IDLE) state.currentBet else null,
                    isActive = state.status == GameStatus.PLAYING
                ) {
                    HandRow(state.playerHand)
                }
            }
        }

        // Right side: Status and Actions
        Column(
            modifier = Modifier.weight(0.8f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (state.status != GameStatus.PLAYING &&
                state.status != GameStatus.BETTING &&
                state.status != GameStatus.INSURANCE_OFFERED
            ) {
                GameStatusMessage(status = state.status, pulseScale = pulseScale, isCompact = true)
                Spacer(modifier = Modifier.height(16.dp))
            }

            GameActions(
                state = state,
                audioService = audioService,
                component = component,
                isCompact = true
            )
        }
    }
}

@Composable
private fun GameStatusMessage(
    status: GameStatus,
    pulseScale: Float,
    isCompact: Boolean
) {
    val statusText =
        when (status) {
            GameStatus.BETTING -> stringResource(Res.string.status_betting)
            GameStatus.IDLE -> stringResource(Res.string.status_idle)
            GameStatus.PLAYING -> stringResource(Res.string.status_playing)
            GameStatus.DEALER_TURN -> stringResource(Res.string.status_dealer_turn)
            GameStatus.PLAYER_WON -> stringResource(Res.string.status_player_won)
            GameStatus.DEALER_WON -> stringResource(Res.string.status_dealer_won)
            GameStatus.PUSH -> stringResource(Res.string.status_push)
            else -> ""
        }
    Text(
        text = statusText,
        style =
            if (isCompact) {
                MaterialTheme.typography.displaySmall
            } else {
                MaterialTheme.typography.displayMedium
            },
        color = PrimaryGold,
        fontWeight = FontWeight.Black,
        modifier =
            Modifier.graphicsLayer {
                scaleX = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
                scaleY = if (status == GameStatus.PUSH || status == GameStatus.PLAYER_WON) pulseScale else 1f
            }
    )
}

@Composable
private fun BetChip(
    amount: Int,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.2f)
    val backgroundColor = if (isActive) PrimaryGold else PrimaryGold.copy(alpha = 0.4f)
    val textColor = if (isActive) BackgroundDark else BackgroundDark.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(backgroundColor, RoundedCornerShape(24.dp))
                .border(2.dp, borderColor, RoundedCornerShape(24.dp)) // Note: Dash effect not directly in border but good enough for now, or use Canvas
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(2.dp, BackgroundDark.copy(alpha = 0.1f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$${amount}",
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
fun GameActions(
    state: GameState,
    audioService: AudioService,
    component: BlackjackComponent,
    isCompact: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (state.status == GameStatus.PLAYING) {
            // Secondary Actions Row
            val canSplit = state.canSplit()
            val canDouble = state.canDoubleDown()
            if (canSplit || canDouble) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally)
                ) {
                    if (canDouble) {
                        ActionIcon(icon = "x2", label = "Double") {
                            audioService.playEffect(AudioService.SoundEffect.DEAL)
                            component.onAction(GameAction.DoubleDown)
                        }
                    }
                    if (canSplit) {
                        ActionIcon(icon = "⑃", label = "Split") {
                            audioService.playEffect(AudioService.SoundEffect.DEAL)
                            component.onAction(GameAction.Split)
                        }
                    }
                }
            }

            // Primary Actions Grid
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CasinoButton(
                    text = stringResource(Res.string.hit),
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.DEAL)
                        component.onAction(GameAction.Hit)
                    },
                    modifier = Modifier.weight(1f),
                    isStrategic = true
                )
                CasinoButton(
                    text = stringResource(Res.string.stand),
                    onClick = {
                        audioService.playEffect(AudioService.SoundEffect.CLICK)
                        component.onAction(GameAction.Stand)
                    },
                    modifier = Modifier.weight(1f),
                    containerColor = GlassDark,
                    contentColor = Color.White
                )
            }
        } else if (state.status != GameStatus.INSURANCE_OFFERED) {
            CasinoButton(
                text = stringResource(Res.string.deal),
                onClick = {
                    audioService.playEffect(AudioService.SoundEffect.FLIP)
                    component.onAction(GameAction.NewGame())
                },
                modifier = Modifier.fillMaxWidth(),
                isStrategic = true
            )
        }
    }
}

@Composable
private fun ActionIcon(icon: String, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    ) {
        Box(
            modifier =
                Modifier
                    .size(48.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun Header(balance: Int) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(GlassDark)
                .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "BALANCE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = "$${balance}.00",
                style = MaterialTheme.typography.headlineSmall,
                color = PrimaryGold,
                fontWeight = FontWeight.Black
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HeaderIcon("history")
            HeaderIcon("settings")
        }
    }
}

@Composable
private fun HeaderIcon(text: String) {
    Box(
        modifier =
            Modifier
                .size(40.dp)
                .background(GlassDark, RoundedCornerShape(20.dp))
                .border(1.dp, GlassLight, RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Placeholder for real icons, using emoji/text for now
        Text(
            text = if (text == "settings") "⚙️" else "🕒",
            fontSize = 18.sp
        )
    }
}

@Composable
private fun HandContainer(
    title: String,
    score: Int,
    bet: Int? = null,
    isActive: Boolean = false,
    isPending: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val borderColor = if (isActive) PrimaryGold else if (isPending) GlassLight else Color.White.copy(alpha = 0.05f)
    val backgroundColor = if (isActive) PrimaryGold.copy(alpha = 0.1f) else if (isPending) Color.Black.copy(alpha = 0.2f) else GlassDark.copy(alpha = 0.3f)

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(backgroundColor)
                .border(if (isActive) 2.dp else 1.dp, borderColor, RoundedCornerShape(24.dp))
                .padding(vertical = 20.dp, horizontal = 16.dp)
    ) {
        if (isActive) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .background(PrimaryGold, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "ACTIVE",
                    color = BackgroundDark,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        } else if (isPending) {
            Box(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = (-32).dp)
                        .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PENDING",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(start = 24.dp)
                ) {
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.displayMedium,
                        color = if (isActive) PrimaryGold else Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Black
                    )
                    if (bet != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        BetChip(amount = bet, isActive = isActive)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive) PrimaryGold.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.3f),
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun HandRow(hand: Hand) {
    Row(
        horizontalArrangement = Arrangement.spacedBy((-40).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        hand.cards.forEach { card ->
            key(card) {
                PlayingCard(
                    card = card,
                    isFaceUp = !card.isFaceDown
                )
            }
        }
    }
}
