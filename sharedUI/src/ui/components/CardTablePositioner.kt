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
    val scale: Float,
    val animDelay: Int,
    val animDuration: Int,
)

data class HandZone(
    val handIndex: Int, // -1 = dealer; 0..N-1 = player hands
    val clusterCenter: Offset,
    val clusterTopLeft: Offset,
    val clusterSize: Size,
    val scale: Float,
)

data class TableLayout(
    val cardSlots: List<CardSlotLayout>,
    val handZones: List<HandZone>, // index 0 = dealer, 1..N = player hands
)

private object TableMetrics {
    const val DEALER_ZONE_CENTER_Y_RATIO = 0.22f
    const val PLAYER_ZONE_CENTER_Y_RATIO = 0.72f
    const val PLAYER_SMILE_ARC_RADIUS_RATIO = 0.12f
    const val INTRA_HAND_SMILE_RADIUS_RATIO = 0.06f
    const val FAN_ANGLE_DEGREES_PER_CARD = 6f
    const val DEALER_OVERLAP_FACTOR = 0.22f
    const val PLAYER_OVERLAP_FACTOR_SINGLE = 0.30f
    const val PLAYER_OVERLAP_FACTOR_MULTI = 0.25f
    const val SQUEEZED_MIN_VISIBLE_WIDTH_SINGLE = 0.32f
    const val SQUEEZED_MIN_VISIBLE_WIDTH_MULTI = 0.20f
    const val DEALER_AVAILABLE_WIDTH_RATIO = 0.4f
}

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

    val baseCardWPx = with(density) { Dimensions.Card.StandardWidth.toPx() }
    val baseStepYPx = with(density) { 6.dp.toPx() }

    val cardW = baseCardWPx * cardScale
    val cardH = cardW / Dimensions.Card.AspectRatio

    // Casino-authentic overlap: only the index strip is visible (~22–25%)
    val playerOverlapFactor =
        if (nPlayerHands >
            1
        ) {
            TableMetrics.PLAYER_OVERLAP_FACTOR_MULTI
        } else {
            TableMetrics.PLAYER_OVERLAP_FACTOR_SINGLE
        }
    val stepYPx = baseStepYPx * cardScale // Slightly flatter stacks

    val minVisibleWidthFactor =
        if (nPlayerHands > 1) TableMetrics.SQUEEZED_MIN_VISIBLE_WIDTH_MULTI
        else TableMetrics.SQUEEZED_MIN_VISIBLE_WIDTH_SINGLE

    val dealerParams = SlotParams(
        cardW = cardW,
        cardH = cardH,
        defaultStepX = cardW * TableMetrics.DEALER_OVERLAP_FACTOR,
        stepYPx = stepYPx,
        cardScale = cardScale,
        minVisibleWidthFactor = TableMetrics.SQUEEZED_MIN_VISIBLE_WIDTH_SINGLE,
    )
    val playerParams = SlotParams(
        cardW = cardW,
        cardH = cardH,
        defaultStepX = cardW * playerOverlapFactor,
        stepYPx = stepYPx,
        cardScale = cardScale,
        minVisibleWidthFactor = minVisibleWidthFactor,
    )

    val cardSlots = mutableListOf<CardSlotLayout>()
    val handZones = mutableListOf<HandZone>()

    // Dealer cards are indexed first so they animate before player cards.
    // This is intentional: the declarative animation system keys on composition
    // order, and dealer-then-player produces a clean sweep rather than interleaving.
    var globalCardIndex = 0

    // Dealer zone
    val dealerZoneCenter = Offset(areaWidth / 2f, areaHeight * TableMetrics.DEALER_ZONE_CENTER_Y_RATIO)
    val (dealerSlots, dealerZone) =
        computeZone(
            cards = state.dealerHand.cards,
            handIndex = -1,
            zoneCenter = dealerZoneCenter,
            shoePosition = shoePosition,
            availableWidth = areaWidth * TableMetrics.DEALER_AVAILABLE_WIDTH_RATIO, // Dealer is constrained to a centered strip; player zones own the horizontal spread
            params = dealerParams,
            isDealer = true,
            startIndex = globalCardIndex,
        )
    cardSlots.addAll(dealerSlots)
    handZones.add(dealerZone)
    globalCardIndex += dealerSlots.size

    // Player zones: implement a "smiling" radial arc
    val zoneWidth = areaWidth / nPlayerHands
    val arcRadius = areaHeight * TableMetrics.PLAYER_SMILE_ARC_RADIUS_RATIO // The "depth" of the smile

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
        val playerZoneCenter = Offset(zoneCenterX, areaHeight * TableMetrics.PLAYER_ZONE_CENTER_Y_RATIO + arcYOffset)

        val (playerSlots, playerZone) =
            computeZone(
                cards = hand.cards,
                handIndex = handIdx,
                zoneCenter = playerZoneCenter,
                shoePosition = shoePosition,
                availableWidth = zoneWidth * 0.95f,
                params = playerParams,
                isDealer = false,
                startIndex = globalCardIndex,
            )
        cardSlots.addAll(playerSlots)
        handZones.add(playerZone)
        globalCardIndex += playerSlots.size
    }

    return TableLayout(cardSlots = cardSlots, handZones = handZones)
}

private data class SlotParams(
    val cardW: Float,
    val cardH: Float,
    val defaultStepX: Float,
    val stepYPx: Float,
    val cardScale: Float,
    val minVisibleWidthFactor: Float,
)

private fun computeZone(
    cards: List<Card>,
    handIndex: Int,
    zoneCenter: Offset,
    shoePosition: Offset,
    availableWidth: Float,
    params: SlotParams,
    isDealer: Boolean,
    startIndex: Int,
): Pair<List<CardSlotLayout>, HandZone> {
    val (cardW, cardH, defaultStepX, stepYPx, cardScale, minVisibleWidthFactor) = params
    val n = cards.size
    val actualStepX = squeezedStep(n, cardW, defaultStepX, availableWidth, minVisibleWidthFactor)
    val totalW = clusterWidth(n, cardW, actualStepX)
    val totalH = clusterHeight(n, cardH, stepYPx)

    val smileRadius = cardH * TableMetrics.INTRA_HAND_SMILE_RADIUS_RATIO

    val slots =
        cards.mapIndexed { index, card ->
            val cx = zoneCenter.x - totalW / 2f + index * actualStepX + cardW / 2f
            val relPos = if (n > 1) (index / (n - 1f)) * 2f - 1f else 0f
            val smileYOffset = relPos * relPos * smileRadius
            val cy = zoneCenter.y - totalH / 2f + index * stepYPx + cardH / 2f + smileYOffset
            val fanAngle = (index - (n - 1) / 2f) * TableMetrics.FAN_ANGLE_DEGREES_PER_CARD

            val animDelay = (startIndex + index) * AnimationConstants.CardDealDelay

            CardSlotLayout(
                card = card,
                handIndex = handIndex,
                cardIndex = index,
                startOffset = shoePosition,
                centerOffset = Offset(cx, cy),
                rotationZ = fanAngle,
                isFaceUp = card.isFaceUp,
                isDealer = isDealer,
                scale = cardScale,
                animDelay = animDelay,
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
            scale = cardScale,
        )

    return Pair(slots, zone)
}

private fun squeezedStep(
    n: Int,
    cardW: Float,
    defaultStepX: Float,
    availableWidth: Float,
    minVisibleWidthFactor: Float,
): Float {
    if (n <= 1) return defaultStepX
    val requiredW = cardW + (n - 1) * defaultStepX
    return if (requiredW > availableWidth) {
        val squeezed = (availableWidth - cardW) / (n - 1)
        val minVisibleWidth = cardW * minVisibleWidthFactor
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
