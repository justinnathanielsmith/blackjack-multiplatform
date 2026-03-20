package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.GameState
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.Dimensions

data class CardSlotLayout(
    val card: Card,
    val handIndex: Int, // -1 = dealer
    val cardIndex: Int,
    val startOffset: Offset,
    val centerOffset: Offset, // gameplay-area local coords
    val rotationZ: Float,
    val isFaceUp: Boolean,
    val isDealer: Boolean,
    val isHoleCard: Boolean,
    val scale: Float,
    val animDelay: Int,
    val animDuration: Int,
)

data class HandZone(
    val handIndex: Int, // -1 = dealer; 0..N-1 = player hands
    val clusterCenter: Offset,
    val clusterTopLeft: Offset,
    val clusterSize: Size,
)

data class TableLayout(
    val cardSlots: List<CardSlotLayout>,
    val handZones: List<HandZone>, // index 0 = dealer, 1..N = player hands
)

private data class ZoneMetrics(
    val stepX: Float,
    val totalW: Float,
    val totalH: Float,
)

fun computeTableLayout(
    state: GameState,
    areaWidth: Float,
    areaHeight: Float,
    density: Density,
    shoePosition: Offset,
): TableLayout {
    val nPlayerHands = state.playerHands.size.coerceAtLeast(1)

    // Dynamic Scaling: Reduced for 3 hands to save space
    val cardScale =
        when (nPlayerHands) {
            1 -> 1.0f
            2 -> 0.82f
            else -> 0.52f // Optimized from 0.55f for 3 hands
        }

    val cardW = with(density) { Dimensions.Card.StandardWidth.toPx() } * cardScale
    val cardH = cardW / Dimensions.Card.AspectRatio

    // Casino-authentic overlap: only the index strip is visible (~22–25%)
    val dealerOverlapFactor = 0.22f
    val playerOverlapFactor = if (nPlayerHands > 1) 0.25f else 0.30f
    val stepYPx = with(density) { 6.dp.toPx() } * cardScale // Slightly flatter stacks

    val sharedParams = SlotParams(cardW, cardH, cardW * dealerOverlapFactor, stepYPx, cardScale)

    val cardSlots = mutableListOf<CardSlotLayout>()
    val handZones = mutableListOf<HandZone>()

    // Dealer zone
    val dealerZoneCenter = Offset(areaWidth / 2f, areaHeight * 0.22f)
    val (dealerSlots, dealerZone) =
        computeZone(
            cards = state.dealerHand.cards,
            handIndex = -1,
            zoneCenter = dealerZoneCenter,
            shoePosition = shoePosition,
            availableWidth = areaWidth * 0.4f, // Dealer has less horizontal space constraint
            params = sharedParams,
            isDealer = true,
        )
    cardSlots.addAll(dealerSlots)
    handZones.add(dealerZone)

    // Player zones: implement a "smiling" radial arc
    val zoneWidth = areaWidth / nPlayerHands
    val arcRadius = areaHeight * 0.12f // The "depth" of the smile
    val activeHandIndex = state.activeHandIndex

    state.playerHands.forEachIndexed { handIdx, hand ->
        // Normalize hand index to -1.0 to 1.0 range
        val relativePos =
            if (nPlayerHands > 1) {
                (handIdx / (nPlayerHands - 1f)) * 2f - 1f
            } else {
                0f
            }

        val zoneCenterX = (handIdx + 0.5f) * zoneWidth
        // Parabolic arc: y = x^2 * radius
        val arcYOffset = relativePos * relativePos * arcRadius
        val playerZoneCenter = Offset(zoneCenterX, areaHeight * 0.72f + arcYOffset)

        // Active hand gets a slight scale boost to reinforce the spotlight effect
        val isActive = handIdx == activeHandIndex
        val handScale = if (isActive && nPlayerHands > 1) cardScale * 1.1f else cardScale
        val handCardW = with(density) { Dimensions.Card.StandardWidth.toPx() } * handScale
        val handCardH = handCardW / Dimensions.Card.AspectRatio
        val handParams =
            SlotParams(
                cardW = handCardW,
                cardH = handCardH,
                defaultStepX = handCardW * playerOverlapFactor,
                stepYPx = with(density) { 6.dp.toPx() } * handScale,
                cardScale = handScale,
            )

        val (playerSlots, playerZone) =
            computeZone(
                cards = hand.cards,
                handIndex = handIdx,
                zoneCenter = playerZoneCenter,
                shoePosition = shoePosition,
                availableWidth = zoneWidth * 0.95f,
                params = handParams,
                isDealer = false,
                nPlayerHands = nPlayerHands,
            )
        cardSlots.addAll(playerSlots)
        handZones.add(playerZone)
    }

    return TableLayout(cardSlots = cardSlots, handZones = handZones)
}

private data class SlotParams(
    val cardW: Float,
    val cardH: Float,
    val defaultStepX: Float,
    val stepYPx: Float,
    val cardScale: Float,
)

private fun computeZone(
    cards: List<Card>,
    handIndex: Int,
    zoneCenter: Offset,
    shoePosition: Offset,
    availableWidth: Float,
    params: SlotParams,
    isDealer: Boolean,
    nPlayerHands: Int = 1,
): Pair<List<CardSlotLayout>, HandZone> {
    val (cardW, cardH, defaultStepX, stepYPx, cardScale) = params
    val n = cards.size
    val actualStepX = squeezedStep(n, cardW, defaultStepX, availableWidth, nPlayerHands)
    val totalW = clusterWidth(n, cardW, actualStepX)
    val totalH = clusterHeight(n, cardH, stepYPx)

    val smileRadius = cardH * 0.06f // 6% of card height — subtle intra-hand arc

    val slots =
        cards.mapIndexed { index, card ->
            val cx = zoneCenter.x - totalW / 2f + index * actualStepX + cardW / 2f
            val relPos = if (n > 1) (index / (n - 1f)) * 2f - 1f else 0f
            val smileYOffset = relPos * relPos * smileRadius
            val cy = zoneCenter.y - totalH / 2f + index * stepYPx + cardH / 2f + smileYOffset
            val fanAngle = (index - (n - 1) / 2f) * 6f
            CardSlotLayout(
                card = card,
                handIndex = handIndex,
                cardIndex = index,
                startOffset = shoePosition,
                centerOffset = Offset(cx, cy),
                rotationZ = fanAngle,
                isFaceUp = !card.isFaceDown,
                isDealer = isDealer,
                isHoleCard = isDealer && index == 1,
                scale = cardScale,
                animDelay = index * AnimationConstants.CardDealDelay,
                animDuration = AnimationConstants.CardRevealDurationDefault,
            )
        }

    val topLeft = Offset(zoneCenter.x - totalW / 2f, zoneCenter.y - totalH / 2f)
    val zone =
        HandZone(
            handIndex = handIndex,
            clusterCenter = zoneCenter,
            clusterTopLeft = topLeft,
            clusterSize = Size(totalW.coerceAtLeast(cardW), totalH.coerceAtLeast(cardH)),
        )

    return Pair(slots, zone)
}

private fun squeezedStep(
    n: Int,
    cardW: Float,
    defaultStepX: Float,
    availableWidth: Float,
    nPlayerHands: Int = 1,
): Float {
    if (n <= 1) return defaultStepX
    val requiredW = cardW + (n - 1) * defaultStepX
    return if (requiredW > availableWidth) {
        val squeezed = (availableWidth - cardW) / (n - 1)
        // Allow tighter overlap when multiple hands share the screen
        val minVisibleWidth = if (nPlayerHands > 1) cardW * 0.20f else cardW * 0.32f
        squeezed.coerceAtLeast(minVisibleWidth)
    } else {
        defaultStepX
    }
}

private fun clusterWidth(
    n: Int,
    cardW: Float,
    stepX: Float
) = if (n <= 1) cardW else cardW + (n - 1) * stepX

private fun clusterHeight(
    n: Int,
    cardH: Float,
    stepY: Float
) = if (n <= 1) cardH else cardH + (n - 1) * stepY
