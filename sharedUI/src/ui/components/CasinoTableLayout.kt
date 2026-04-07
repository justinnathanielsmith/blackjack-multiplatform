package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.util.lerp
import io.github.smithjustinn.blackjack.GameState
import kotlin.math.roundToInt

data class CardParentData(
    val id: String = "",
    val flightProgress: State<Float>? = null,
)

class FlightProgressModifier(
    val progress: State<Float>
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        (parentData as? CardParentData ?: CardParentData()).copy(flightProgress = progress)
}

fun Modifier.flightProgress(progress: State<Float>) = this.then(FlightProgressModifier(progress))

class NodeIdModifier(
    val id: String
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any =
        (parentData as? CardParentData ?: CardParentData()).copy(id = id)
}

fun Modifier.nodeId(id: String) = this.then(NodeIdModifier(id))

@Composable
fun CasinoTableLayout(
    state: GameState,
    shoePosition: Offset,
    modifier: Modifier = Modifier,
    gameplayAreaHeight: Float = 0f,
    onLayout: ((TableLayout) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val areaWidth = constraints.maxWidth.toFloat()
        // Use the actual gameplay area height when available (first frame = 0f → skip).
        val areaHeight =
            if (gameplayAreaHeight > 0f) {
                gameplayAreaHeight
            } else {
                constraints.maxHeight.toFloat()
            }

        // Run the math in the layout phase! Bypasses Composition when dimensions change.
        val tableLayout = computeTableLayout(state, areaWidth, areaHeight, density, shoePosition)
        // Publish so BlackjackScreen can use zone positions for payout / highlight animations.
        onLayout?.invoke(tableLayout)

        // Build a lookup from handIndex → zone for HUD/glow measurement.
        val zoneByIndex = tableLayout.handZones.associateBy { it.handIndex }

        val placeables =
            measurables.associate { measurable ->
                val id = (measurable.parentData as? CardParentData)?.id ?: ""
                val measureConstraints =
                    when {
                        // HUD must be exactly the cluster size so fillMaxSize() + Alignment.*
                        // inside HandZoneHud resolve to the correct bounds.
                        id.startsWith("hud-") -> {
                            val idx = id.removePrefix("hud-").toIntOrNull()
                            val zone = if (idx != null) zoneByIndex[idx] else null
                            if (zone != null) {
                                val w =
                                    zone.clusterSize.width
                                        .roundToInt()
                                        .coerceAtLeast(1)
                                val h =
                                    zone.clusterSize.height
                                        .roundToInt()
                                        .coerceAtLeast(1)
                                Constraints.fixed(w, h)
                            } else {
                                constraints
                            }
                        }
                        // Glow is drawn at 1.6× cluster size — measure to match.
                        id.startsWith("glow-") -> {
                            val idx = id.removePrefix("glow-").toIntOrNull()
                            val zone = if (idx != null) zoneByIndex[idx] else null
                            if (zone != null) {
                                val w = (zone.clusterSize.width * 1.6f).roundToInt().coerceAtLeast(1)
                                val h = (zone.clusterSize.height * 1.6f).roundToInt().coerceAtLeast(1)
                                Constraints.fixed(w, h)
                            } else {
                                constraints
                            }
                        }
                        // Cards and chips pin their own sizes via requiredSize / requiredWidth.
                        else -> constraints
                    }
                id to measurable.measure(measureConstraints)
            }

        layout(constraints.maxWidth, constraints.maxHeight) {
            // Bolt Performance Optimization: 0-allocation layout phase.
            // Avoid `filter` and iterators by using index-based loops.
            for (i in tableLayout.cardSlots.indices) {
                val slot = tableLayout.cardSlots[i]
                if (slot.isDealer) {
                    val p = placeables["dealer-card-${slot.cardIndex}"]
                    if (p != null) {
                        val destX = slot.centerOffset.x - p.width / 2
                        val destY = slot.centerOffset.y - p.height / 2
                        p.placeWithLayer(destX.roundToInt(), destY.roundToInt(), zIndex = slot.cardIndex.toFloat()) {
                            val progress = (p.parentData as? CardParentData)?.flightProgress?.value ?: 1f
                            val startX = shoePosition.x - destX
                            val startY = shoePosition.y - destY
                            translationX = lerp(startX, 0f, progress)
                            translationY = lerp(startY, 0f, progress)
                            rotationZ = lerp(-45f, slot.rotationZ, progress)
                            val lerpedScale = lerp(0.5f, slot.scale, progress)
                            scaleX = lerpedScale
                            scaleY = lerpedScale
                        }
                    }
                } else {
                    val p = placeables["player-card-${slot.handIndex}-${slot.cardIndex}"]
                    if (p != null) {
                        val destX = slot.centerOffset.x - p.width / 2
                        val destY = slot.centerOffset.y - p.height / 2
                        p.placeWithLayer(destX.roundToInt(), destY.roundToInt(), zIndex = slot.cardIndex.toFloat()) {
                            val progress = (p.parentData as? CardParentData)?.flightProgress?.value ?: 1f
                            val startX = shoePosition.x - destX
                            val startY = shoePosition.y - destY
                            translationX = lerp(startX, 0f, progress)
                            translationY = lerp(startY, 0f, progress)
                            rotationZ = lerp(45f, slot.rotationZ, progress)
                            val lerpedScale = lerp(0.5f, slot.scale, progress)
                            scaleX = lerpedScale
                            scaleY = lerpedScale
                        }
                    }
                }
            }

            // Place chips
            for (i in tableLayout.chipSlots.indices) {
                val slot = tableLayout.chipSlots[i]
                val p = placeables["chip-${slot.handIndex}"]
                if (p != null) {
                    val destX = slot.centerOffset.x - p.width / 2
                    val destY = slot.centerOffset.y - p.height / 2
                    p.placeWithLayer(destX.roundToInt(), destY.roundToInt(), zIndex = 20f) {
                        val progress = (p.parentData as? CardParentData)?.flightProgress?.value ?: 1f
                        val startX = slot.startOffset.x - destX
                        val startY = slot.startOffset.y - destY
                        translationX = lerp(startX, 0f, progress)
                        translationY = lerp(startY, 0f, progress)
                        scaleX = slot.scale
                        scaleY = slot.scale
                    }
                }
            }

            // Place hand zones (HUD, Glow)
            for (i in tableLayout.handZones.indices) {
                val zone = tableLayout.handZones[i]
                val glowP = placeables["glow-${zone.handIndex}"]
                if (glowP != null) {
                    val glowW = zone.clusterSize.width * 1.6f
                    val glowH = zone.clusterSize.height * 1.6f
                    val gx = zone.clusterCenter.x - glowW / 2f
                    val gy = zone.clusterCenter.y - glowH / 2f
                    glowP.placeWithLayer(gx.roundToInt(), gy.roundToInt(), zIndex = -1f)
                }

                val hudP = placeables["hud-${zone.handIndex}"]
                if (hudP != null) {
                    hudP.placeWithLayer(
                        x = zone.clusterTopLeft.x.roundToInt(),
                        y = zone.clusterTopLeft.y.roundToInt(),
                        zIndex = 100f
                    )
                }
            }
        }
    }
}
