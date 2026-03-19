package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.Card
import io.github.smithjustinn.blackjack.ui.components.CardBack
import io.github.smithjustinn.blackjack.ui.components.CardCorner
import io.github.smithjustinn.blackjack.ui.components.CardFace
import io.github.smithjustinn.blackjack.ui.components.CardShape
import io.github.smithjustinn.blackjack.ui.components.color
import io.github.smithjustinn.blackjack.ui.components.symbol
import io.github.smithjustinn.blackjack.ui.theme.Dimensions
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class FlyingCardInstance(
    val id: Long,
    val card: Card,
    val isFaceUp: Boolean,
    val scale: Float,
    val startOffset: Offset,
    val endOffset: Offset,
    val animationDelay: Int,
    val durationMs: Int,
    val targetRotationZ: Float,
    val isDealer: Boolean,
)

class DealAnimationRegistry {
    val flyingCards = mutableStateListOf<FlyingCardInstance>()
    private val landedCards = mutableStateMapOf<Card, Unit>()
    var overlayOffset by mutableStateOf(Offset.Zero)

    fun isLanded(card: Card): Boolean = card in landedCards

    fun requestDeal(instance: FlyingCardInstance) {
        if (!isLanded(instance.card) && flyingCards.none { it.card == instance.card }) {
            flyingCards.add(instance)
        }
    }

    fun markLanded(card: Card) {
        landedCards[card] = Unit
        flyingCards.removeAll { it.card == card }
    }

    fun reset() {
        flyingCards.clear()
        landedCards.clear()
    }
}

val LocalDealAnimationRegistry = staticCompositionLocalOf { DealAnimationRegistry() }

@Composable
fun FlyingCard(
    instance: FlyingCardInstance,
    onAnimationEnd: () -> Unit
) {
    val registry = LocalDealAnimationRegistry.current
    val currentX = remember(instance.id) { Animatable(instance.startOffset.x, visibilityThreshold = 1f) }
    val currentY = remember(instance.id) { Animatable(instance.startOffset.y, visibilityThreshold = 1f) }
    val rotationAnim =
        remember(instance.id) { Animatable(if (instance.isDealer) -45f else 45f, visibilityThreshold = 0.1f) }
    val scale = remember(instance.id) { Animatable(0.5f) }

    LaunchedEffect(instance.id) {
        delay(instance.animationDelay.toLong())

        // Calculate stiffness to settle visually (to 1px) within durationMs
        // omega_n = ln(ratio) / (zeta * duration)
        // stiffness = omega_n^2
        // For ratio=1000 (1000px to 1px), ln(ratio) approx 6.9
        val zeta = 0.65f
        val durationSec = instance.durationMs / 1000f
        val stiffness =
            if (durationSec > 0) {
                val omegaN = 6.9f / (zeta * durationSec)
                (omegaN * omegaN).coerceIn(10f, 2000f)
            } else {
                Spring.StiffnessMedium
            }

        val jobs =
            listOf(
                launch { currentX.animateTo(instance.endOffset.x, spring(dampingRatio = zeta, stiffness = stiffness)) },
                launch { currentY.animateTo(instance.endOffset.y, spring(dampingRatio = zeta, stiffness = stiffness)) },
                launch {
                    rotationAnim.animateTo(
                        instance.targetRotationZ,
                        spring(dampingRatio = 0.6f, stiffness = stiffness)
                    )
                },
                launch {
                    scale.animateTo(1.15f, tween(150))
                    scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium))
                },
            )
        jobs.joinAll()
        onAnimationEnd()
    }

    val composeDensity = LocalDensity.current
    val baseCardWidthPx = with(composeDensity) { Dimensions.Card.StandardWidth.toPx() }
    val baseCardHeightPx = baseCardWidthPx / Dimensions.Card.AspectRatio
    val overlayOffset = registry.overlayOffset

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .requiredWidth(Dimensions.Card.StandardWidth)
                    .aspectRatio(Dimensions.Card.AspectRatio)
                    .graphicsLayer {
                        val visualScale = scale.value * instance.scale
                        // Centering fix: the box is scaled around its center (baseCardWidthPx/2),
                        // so to place its center at currentX.value, we just subtract baseCardWidthPx/2.
                        translationX = currentX.value - baseCardWidthPx / 2 - overlayOffset.x
                        translationY = currentY.value - baseCardHeightPx / 2 - overlayOffset.y
                        scaleX = visualScale
                        scaleY = visualScale
                        rotationZ = rotationAnim.value
                        cameraDistance = 12f * density
                    },
        ) {
            if (instance.isFaceUp) {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = CardShape,
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val cardWidth = maxWidth
                        val isSmall = cardWidth < Dimensions.Card.SmallCardThreshold
                        val cornerPadding = if (isSmall) 4.dp else 6.dp

                        Box(modifier = Modifier.fillMaxSize().padding(cornerPadding)) {
                            if (isSmall) {
                                CardCorner(
                                    rank = instance.card.rank.symbol,
                                    suit = instance.card.suit.symbol,
                                    color = instance.card.suit.color,
                                    isSmall = true,
                                    modifier = Modifier.align(Alignment.TopStart),
                                )
                            } else {
                                CardCorner(
                                    rank = instance.card.rank.symbol,
                                    suit = instance.card.suit.symbol,
                                    color = instance.card.suit.color,
                                    isSmall = false,
                                    modifier = Modifier.align(Alignment.TopStart),
                                )
                                CardFace(
                                    rank = instance.card.rank,
                                    suit = instance.card.suit,
                                    modifier = Modifier.fillMaxSize(),
                                )
                                CardCorner(
                                    rank = instance.card.rank.symbol,
                                    suit = instance.card.suit.symbol,
                                    color = instance.card.suit.color,
                                    isSmall = false,
                                    modifier = Modifier.align(Alignment.BottomEnd).rotate(180f),
                                )
                            }
                        }
                    }
                }
            } else {
                CardBack()
            }
        }
    }
}
