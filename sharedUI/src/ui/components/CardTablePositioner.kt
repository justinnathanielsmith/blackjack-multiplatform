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
): TableLayout {
    val nPlayerHands = state.playerHands.size.coerceAtLeast(1)
    val cardScale =
        when (nPlayerHands) {
            1 -> 1.0f
            2 -> 0.80f
            else -> 0.55f
        }

    val cardW = with(density) { Dimensions.Card.StandardWidth.toPx() } * cardScale
    val cardH = cardW / Dimensions.Card.AspectRatio
    val overlapOffsetPx =
        with(density) {
            Dimensions.Card.OverlapOffsetRaw.dp
                .toPx()
        } * cardScale
    val defaultStepX = overlapOffsetPx + cardW
    val stepYPx = with(density) { 8.dp.toPx() } * cardScale

    val sharedParams = SlotParams(cardW, cardH, defaultStepX, stepYPx, cardScale)

    val cardSlots = mutableListOf<CardSlotLayout>()
    val handZones = mutableListOf<HandZone>()

    // Dealer zone
    val dealerZoneCenter = Offset(areaWidth / 2f, areaHeight * 0.22f)
    val (dealerSlots, dealerZone) =
        computeZone(
            cards = state.dealerHand.cards,
            handIndex = -1,
            zoneCenter = dealerZoneCenter,
            availableWidth = areaWidth,
            params = sharedParams,
            isDealer = true,
        )
    cardSlots.addAll(dealerSlots)
    handZones.add(dealerZone)

    // Player zones
    val zoneWidth = areaWidth / nPlayerHands
    state.playerHands.forEachIndexed { handIdx, hand ->
        val zoneCenterX = (handIdx + 0.5f) * zoneWidth
        val playerZoneCenter = Offset(zoneCenterX, areaHeight * 0.68f)
        val (playerSlots, playerZone) =
            computeZone(
                cards = hand.cards,
                handIndex = handIdx,
                zoneCenter = playerZoneCenter,
                availableWidth = zoneWidth,
                params = sharedParams,
                isDealer = false,
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
    availableWidth: Float,
    params: SlotParams,
    isDealer: Boolean,
): Pair<List<CardSlotLayout>, HandZone> {
    val (cardW, cardH, defaultStepX, stepYPx, cardScale) = params
    val n = cards.size
    val actualStepX = squeezedStep(n, cardW, defaultStepX, availableWidth)
    val totalW = clusterWidth(n, cardW, actualStepX)
    val totalH = clusterHeight(n, cardH, stepYPx)

    val slots =
        cards.mapIndexed { index, card ->
            val cx = zoneCenter.x - totalW / 2f + index * actualStepX + cardW / 2f
            val cy = zoneCenter.y - totalH / 2f + index * stepYPx + cardH / 2f
            val fanAngle = (index - (n - 1) / 2f) * 3f
            CardSlotLayout(
                card = card,
                handIndex = handIndex,
                cardIndex = index,
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
    availableWidth: Float
): Float {
    if (n <= 1) return defaultStepX
    val requiredW = cardW + (n - 1) * defaultStepX
    return if (requiredW > availableWidth) {
        val squeezed = (availableWidth - cardW) / (n - 1)
        squeezed.coerceAtLeast(cardW * 0.32f)
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
