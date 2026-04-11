package io.github.smithjustinn.blackjack.ui.components.cards

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.ui.theme.AnimationConstants
import io.github.smithjustinn.blackjack.ui.theme.BlackjackTheme
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import io.github.smithjustinn.blackjack.ui.theme.PrimaryGold
import org.jetbrains.compose.resources.stringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.playing_card_face_down_description
import sharedui.generated.resources.playing_card_face_up_description

internal val CardShape = RoundedCornerShape(8.dp)

@Composable
fun PlayingCard(
    card: Card,
    isFaceUp: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 1f,
    isNearMiss: Boolean = false,
    isDimmed: Boolean = false,
    shadowElevation: Dp = 6.dp,
    spotColor: Color = Color.Black,
    isDoubleDown: Boolean = false,
) {
    val baseRotation =
        remember(card.rank, card.suit) {
            val hash = (card.rank.hashCode() * 31) + card.suit.hashCode()
            ((hash % 100) / 100f) * 4f - 2f
        }

    val nearMissAlpha = remember { Animatable(0f) }

    LaunchedEffect(isNearMiss) {
        if (isNearMiss) {
            nearMissAlpha.animateTo(1f, tween(durationMillis = AnimationConstants.NearMissInDuration))
            kotlinx.coroutines.delay(AnimationConstants.NearMissHoldDuration)
            nearMissAlpha.animateTo(0f, tween(durationMillis = AnimationConstants.NearMissOutDuration))
        } else {
            nearMissAlpha.snapTo(0f)
        }
    }

    // Double-down convention: 3rd card is placed sideways (90° Z rotation)
    val doubleDownRotation = remember { Animatable(0f) }
    LaunchedEffect(isDoubleDown) {
        if (isDoubleDown) {
            doubleDownRotation.animateTo(
                targetValue = 90f,
                animationSpec =
                    tween(
                        durationMillis = AnimationConstants.CardFlipDuration,
                        easing = FastOutSlowInEasing
                    )
            )
        } else {
            doubleDownRotation.snapTo(0f)
        }
    }

    val transition = updateTransition(targetState = isFaceUp, label = "cardFlip")
    val rotationState =
        transition.animateFloat(
            transitionSpec = {
                tween(durationMillis = AnimationConstants.CardFlipDuration, easing = FastOutSlowInEasing)
            },
            label = "rotation",
        ) { faceUp -> if (faceUp) 180f else 0f }

    // Subtle lift effect as the card is flipped
    val liftScaleState =
        transition.animateFloat(
            transitionSpec = {
                keyframes {
                    durationMillis = AnimationConstants.CardFlipDuration
                    1.0f at 0
                    1.08f at AnimationConstants.CardFlipDuration / 2
                    1.0f at AnimationConstants.CardFlipDuration
                }
            },
            label = "liftScale"
        ) { 1.0f }

    // Bolt Performance Optimization: Use derivedStateOf to prevent O(frames) full recompositions
    val showBack by remember { derivedStateOf { rotationState.value < 90f } }

    val cardDescription =
        if (isFaceUp) {
            val rankName = stringResource(card.rank.nameRes)
            val suitName = stringResource(card.suit.nameRes)
            stringResource(Res.string.playing_card_face_up_description, rankName, suitName)
        } else {
            stringResource(Res.string.playing_card_face_down_description)
        }

    Box(
        modifier =
            modifier
                .clearAndSetSemantics {
                    contentDescription = cardDescription
                }.requiredWidth(Dimensions.Card.StandardWidth * scale)
                .aspectRatio(Dimensions.Card.AspectRatio)
                .graphicsLayer {
                    rotationZ = baseRotation + doubleDownRotation.value
                    rotationY = rotationState.value
                    scaleX = liftScaleState.value
                    scaleY = liftScaleState.value
                    cameraDistance = 12f * density
                },
    ) {
        Card(
            modifier =
                Modifier
                    .fillMaxSize()
                    .shadow(elevation = shadowElevation, shape = CardShape, spotColor = spotColor)
                    .drawWithCache {
                        val normalStroke = Stroke(width = 0.5.dp.toPx())
                        val nearMissStroke = Stroke(width = 2.dp.toPx())
                        val cornerRadius = CornerRadius(8.dp.toPx())
                        onDrawWithContent {
                            drawContent()
                            val alpha = nearMissAlpha.value
                            val borderColor =
                                if (alpha > 0f) PrimaryGold.copy(alpha = alpha) else Color.Black.copy(alpha = 0.1f)
                            drawRoundRect(
                                color = borderColor,
                                size = size,
                                cornerRadius = cornerRadius,
                                style = if (alpha > 0f) nearMissStroke else normalStroke,
                            )
                        }
                    },
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            if (!showBack) {
                BoxWithConstraints(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                ) {
                    val cardWidth = maxWidth
                    val isSmall = cardWidth < Dimensions.Card.SmallCardThreshold
                    val cornerPadding = if (isSmall) 4.dp else 6.dp

                    Box(modifier = Modifier.fillMaxSize().padding(cornerPadding)) {
                        if (isSmall) {
                            CardCorner(
                                rank = stringResource(card.rank.symbolRes),
                                suit = stringResource(card.suit.symbolRes),
                                color = card.suit.color,
                                isSmall = true,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                        } else {
                            CardCorner(
                                rank = stringResource(card.rank.symbolRes),
                                suit = stringResource(card.suit.symbolRes),
                                color = card.suit.color,
                                isSmall = false,
                                modifier = Modifier.align(Alignment.TopStart)
                            )
                            CardFace(
                                rank = card.rank,
                                suit = card.suit,
                                modifier = Modifier.fillMaxSize()
                            )
                            CardCorner(
                                rank = stringResource(card.rank.symbolRes),
                                suit = stringResource(card.suit.symbolRes),
                                color = card.suit.color,
                                isSmall = false,
                                modifier = Modifier.align(Alignment.BottomEnd).rotate(180f)
                            )
                        }
                    }
                    if (isDimmed) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                        )
                    }
                }
            } else {
                CardBack(modifier = Modifier.fillMaxSize())
            }
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun PlayingCardAcePreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlayingCard(
                card = Card(Rank.ACE, Suit.SPADES),
                isFaceUp = true
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun PlayingCardTenPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlayingCard(
                card = Card(Rank.TEN, Suit.DIAMONDS),
                isFaceUp = true
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun PlayingCardCourtPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlayingCard(
                card = Card(Rank.KING, Suit.HEARTS),
                isFaceUp = true
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun PlayingCardBackPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlayingCard(
                card = Card(Rank.TWO, Suit.CLUBS),
                isFaceUp = false
            )
        }
    }
}

@Suppress("UnusedPrivateMember") // Used by Compose Preview
@Preview
@Composable
private fun PlayingCardNearMissPreview() {
    BlackjackTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            PlayingCard(
                card = Card(Rank.ACE, Suit.HEARTS),
                isFaceUp = true,
                isNearMiss = true
            )
        }
    }
}
