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
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.GameState
import io.github.smithjustinn.blackjack.model.GameStatus
import io.github.smithjustinn.blackjack.model.handNetPayout
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
        if (state.status == GameStatus.PLAYING) {
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
        val isDealerActive = state.status == GameStatus.PLAYING && state.activeHandIndex == -1
        state.dealerHand.cards.forEachIndexed { cardIndex, card ->
            val isDimmed = state.status == GameStatus.PLAYING && state.activeHandIndex != -1

            androidx.compose.runtime.key("dealer", cardIndex) {
                PositionedCardItem(
                    card = card,
                    animDelay = 0,
                    isFaceUp = card.isFaceUp,
                    isDealer = true,
                    dealerUpcard = state.dealerHand.cards.getOrNull(0),
                    isDealerBlackjack = state.dealerHand.isBlackjack,
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
            val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
            val isDimmed = state.status == GameStatus.PLAYING && handIndex != state.activeHandIndex
            val isNearMiss = handIndex == nearMissHandIndex

            hand.cards.forEachIndexed { cardIndex, card ->
                val isDoubledCard = hand.isDoubleDown && cardIndex == 2

                androidx.compose.runtime.key("player", handIndex, cardIndex) {
                    PositionedCardItem(
                        card = card,
                        animDelay = 0,
                        isFaceUp = card.isFaceUp,
                        isDealer = false,
                        dealerUpcard = null,
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
        if (state.status != GameStatus.BETTING) {
            state.playerHands.forEachIndexed { handIndex, hand ->
                if (hand.bet > 0) {
                    val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
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
                status = state.status,
                dealerHand = state.dealerHand,
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
            val isActive = state.status == GameStatus.PLAYING && handIndex == state.activeHandIndex
            val result = state.handResult(handIndex)
            val netPayout = state.handNetPayout(handIndex)

            androidx.compose.runtime.key("hud", handIndex) {
                HandZoneHud(
                    status = state.status,
                    dealerHand = state.dealerHand,
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
