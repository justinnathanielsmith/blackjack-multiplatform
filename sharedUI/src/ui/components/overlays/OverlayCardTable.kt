package io.github.smithjustinn.blackjack.ui.components.overlays

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.handNetPayout
import io.github.smithjustinn.blackjack.model.isTerminal
import io.github.smithjustinn.blackjack.ui.components.feedback.HandResult
import io.github.smithjustinn.blackjack.ui.components.feedback.handResult
import io.github.smithjustinn.blackjack.ui.components.layout.CasinoTableLayout
import io.github.smithjustinn.blackjack.ui.components.layout.nodeId
import io.github.smithjustinn.blackjack.ui.effects.LocalDealAnimationRegistry
import io.github.smithjustinn.blackjack.ui.theme.Dimensions

@Composable
fun OverlayCardTable(
    state: GameState,
    nearMissHandIndex: Int?,
    modifier: Modifier = Modifier,
) {
    val registry = LocalDealAnimationRegistry.current
    val density = LocalDensity.current

    val coordOffsetX = registry.gameplayAreaOffset.x - registry.overlayOffset.x
    val coordOffsetY = registry.gameplayAreaOffset.y - registry.overlayOffset.y

    // shoePosition is captured in root coordinates; convert to CasinoTableLayout's
    // local space by subtracting gameplayAreaOffset (the graphicsLayer shift applied
    // to the layout). Without this, cards start ~header-height px below the shoe.
    val shoePositionLocal =
        Offset(
            registry.shoePosition.x - registry.gameplayAreaOffset.x,
            registry.shoePosition.y - registry.gameplayAreaOffset.y,
        )

    val baseCardW = with(density) { Dimensions.Card.StandardWidth.toPx() }
    val baseCardH = baseCardW / Dimensions.Card.AspectRatio

    // The actual height of the gameplay area (between header and ControlCenter),
    // measured by onGloballyPositioned in BlackjackScreen. When 0f (first frame
    // before layout), CasinoTableLayout falls back to the full overlay height.
    val gameplayAreaHeight = registry.gameplayAreaSize.height.toFloat()

    CasinoTableLayout(
        state = state,
        shoePosition = shoePositionLocal,
        gameplayAreaHeight = gameplayAreaHeight,
        onLayout = { registry.tableLayout = it },
        modifier =
            modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = coordOffsetX
                    translationY = coordOffsetY
                }
    ) {
        // 1. Active hand glow behind the cluster
        if (state.isPlayingPhase) {
            val activeHand = state.activeHandIndex
            if (activeHand >= -1) {
                ActiveHandGlow(
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    density = density,
                    modifier = Modifier.nodeId("glow-$activeHand")
                )
            }
        }

        // 2. Positioned (landed) cards - Dealer
        val isDealerActive = state.isDealerActive
        // Pre-map domain rule: slow-roll when dealer has blackjack and upcard is Ace or 10-value
        val upcard = state.dealerHand.cards.getOrNull(0)
        val isSlowRoll =
            state.dealerHand.isBlackjack &&
                upcard != null &&
                (upcard.rank == Rank.ACE || upcard.rank.value == 10)
        state.dealerHand.cards.forEachIndexed { cardIndex, card ->
            val isDimmed = state.isPlayingPhase && !state.isDealerActive

            androidx.compose.runtime.key("dealer", cardIndex) {
                PositionedCardItem(
                    card = card,
                    animDelay = 0,
                    isFaceUp = card.isFaceUp,
                    isDealer = true,
                    isSlowRoll = isSlowRoll,
                    baseCardW = baseCardW,
                    baseCardH = baseCardH,
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    isNearMiss = false,
                    density = density,
                    isActive = isDealerActive,
                    alpha = 1f,
                    isDimmed = isDimmed,
                    cardIndexInHand = cardIndex,
                    modifier = Modifier.nodeId("dealer-card-$cardIndex")
                )
            }
        }

        // 2. Positioned (landed) cards - Player
        state.playerHands.forEachIndexed { handIndex, hand ->
            val isActive = state.isHandActive(handIndex)
            val isDimmed = state.isPlayingPhase && !state.isHandActive(handIndex)
            val isNearMiss = handIndex == nearMissHandIndex

            hand.cards.forEachIndexed { cardIndex, card ->
                val isDoubledCard = hand.isDoubleDown && cardIndex == 2

                androidx.compose.runtime.key("player", handIndex, cardIndex) {
                    PositionedCardItem(
                        card = card,
                        animDelay = 0,
                        isFaceUp = card.isFaceUp,
                        isDealer = false,
                        baseCardW = baseCardW,
                        baseCardH = baseCardH,
                        coordOffsetX = 0f,
                        coordOffsetY = 0f,
                        isNearMiss = isNearMiss,
                        density = density,
                        isActive = isActive,
                        alpha = 1f,
                        isDimmed = isDimmed,
                        cardIndexInHand = cardIndex,
                        isDoubleDown = isDoubledCard,
                        modifier = Modifier.nodeId("player-card-$handIndex-$cardIndex")
                    )
                }
            }
        }

        // 2.5 Positioned (landed) chips — only after cards are dealt
        if (!state.isBettingPhase) {
            state.playerHands.forEachIndexed { handIndex, hand ->
                if (hand.bet > 0) {
                    val isActive = state.isHandActive(handIndex)
                    androidx.compose.runtime.key("chip", handIndex) {
                        PositionedChipItem(
                            amount = hand.bet,
                            handIndex = handIndex,
                            handCount = state.playerHands.size,
                            coordOffsetX = 0f,
                            coordOffsetY = 0f,
                            density = density,
                            isActive = isActive,
                            modifier = Modifier.nodeId("chip-$handIndex")
                        )
                    }
                }
            }
        }

        // 3. HUD badges - Dealer
        androidx.compose.runtime.key("hud", -1) {
            HandZoneHud(
                // Phase-visibility flags from GameState — no status/dealerHand passed into Composable
                isBettingPhase = state.isBettingPhase,
                isDealerBustVisible = state.isDealerBustVisible,
                isDealer21Visible = state.isDealer21Visible,
                isRoundOver = state.status.isTerminal(),
                dealerDisplayScore = state.dealerDisplayScore,
                playerHand = null,
                handResult = HandResult.NONE,
                handNetPayout = null,
                handCount = state.playerHands.size,
                coordOffsetX = 0f,
                coordOffsetY = 0f,
                density = density,
                isActive = isDealerActive,
                isDealer = true,
                handIndex = -1,
                modifier = Modifier.nodeId("hud--1")
            )
        }

        // 3. HUD badges - Players
        state.playerHands.forEachIndexed { handIndex, hand ->
            val isActive = state.isHandActive(handIndex)
            val result = state.handResult(handIndex)
            val netPayout = state.handNetPayouts.getOrNull(handIndex)

            androidx.compose.runtime.key("hud", handIndex) {
                HandZoneHud(
                    // Phase-visibility flags from GameState — no status/dealerHand passed into Composable
                    isBettingPhase = state.isBettingPhase,
                    isDealerBustVisible = state.isDealerBustVisible,
                    isDealer21Visible = state.isDealer21Visible,
                    isRoundOver = state.status.isTerminal(),
                    dealerDisplayScore = state.dealerDisplayScore,
                    playerHand = hand,
                    handResult = result,
                    handNetPayout = netPayout,
                    handCount = state.playerHands.size,
                    coordOffsetX = 0f,
                    coordOffsetY = 0f,
                    density = density,
                    isActive = isActive,
                    isDealer = false,
                    handIndex = handIndex,
                    modifier = Modifier.nodeId("hud-$handIndex")
                )
            }
        }
    }
}
