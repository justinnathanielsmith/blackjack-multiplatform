package io.github.smithjustinn.blackjack.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import io.github.smithjustinn.blackjack.GameState
import kotlin.math.roundToInt
import androidx.compose.ui.util.lerp

class FlightProgressModifier(val progress: State<Float>) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@FlightProgressModifier
}

fun Modifier.flightProgress(progress: State<Float>) = this.then(FlightProgressModifier(progress))

class NodeIdModifier(val id: String) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?) = this@NodeIdModifier
}

fun Modifier.nodeId(id: String) = this.then(NodeIdModifier(id))

@Composable
fun CasinoTableLayout(
    state: GameState,
    shoePosition: Offset,
    // coordOffsetY = gameplayAreaOffset.y - overlayOffset.y (i.e. header height).
    // The custom Layout fills the full overlay, but card positions must be computed
    // relative to the gameplay area only — so we subtract the header offset from
    // the overlay height to get the true gameplay area height.
    coordOffsetY: Float = 0f,
    onLayout: ((TableLayout) -> Unit)? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val areaWidth = constraints.maxWidth.toFloat()
        // Use gameplay-area height, not the full overlay height.
        val areaHeight = (constraints.maxHeight.toFloat() - coordOffsetY).coerceAtLeast(1f)

        // Run the math in the layout phase! Bypasses Composition when dimensions change.
        val tableLayout = computeTableLayout(state, areaWidth, areaHeight, density, shoePosition)
        // Publish so BlackjackScreen can use zone positions for payout / highlight animations.
        onLayout?.invoke(tableLayout)

        val placeables = measurables.associate { measurable ->
            val id = (measurable.parentData as? NodeIdModifier)?.id ?: ""
            id to measurable.measure(constraints) // Chips/Cards specify their own size based on scale
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            // Place dealer cards
            tableLayout.cardSlots.filter { it.isDealer }.forEach { slot ->
                val p = placeables["dealer-card-${slot.cardIndex}"]
                if (p != null) {
                    val destX = slot.centerOffset.x - p.width / 2
                    val destY = slot.centerOffset.y - p.height / 2
                    p.placeWithLayer(destX.roundToInt(), destY.roundToInt()) {
                        val progress = (p.parentData as? FlightProgressModifier)?.progress?.value ?: 1f
                        val startX = shoePosition.x - destX
                        val startY = shoePosition.y - destY
                        translationX = lerp(startX, 0f, progress)
                        translationY = lerp(startY, 0f, progress)
                        rotationZ = lerp(-45f, slot.rotationZ, progress)
                        scaleX = slot.scale
                        scaleY = slot.scale
                    }
                }
            }

            // Place player cards
            tableLayout.cardSlots.filter { !it.isDealer }.forEach { slot ->
                val p = placeables["player-card-${slot.handIndex}-${slot.cardIndex}"]
                if (p != null) {
                    val destX = slot.centerOffset.x - p.width / 2
                    val destY = slot.centerOffset.y - p.height / 2
                    p.placeWithLayer(destX.roundToInt(), destY.roundToInt()) {
                        val progress = (p.parentData as? FlightProgressModifier)?.progress?.value ?: 1f
                        val startX = shoePosition.x - destX
                        val startY = shoePosition.y - destY
                        translationX = lerp(startX, 0f, progress)
                        translationY = lerp(startY, 0f, progress)
                        rotationZ = lerp(45f, slot.rotationZ, progress)
                        scaleX = slot.scale
                        scaleY = slot.scale
                    }
                }
            }

            // Place chips
            tableLayout.chipSlots.forEach { slot ->
                val p = placeables["chip-${slot.handIndex}"]
                if (p != null) {
                    val destX = slot.centerOffset.x - p.width / 2
                    val destY = slot.centerOffset.y - p.height / 2
                    p.placeWithLayer(destX.roundToInt(), destY.roundToInt()) {
                        val progress = (p.parentData as? FlightProgressModifier)?.progress?.value ?: 1f
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
            tableLayout.handZones.forEach { zone ->
                val glowP = placeables["glow-${zone.handIndex}"]
                if (glowP != null) {
                    val glowW = zone.clusterSize.width * 1.6f
                    val glowH = zone.clusterSize.height * 1.6f
                    val gx = zone.clusterCenter.x - glowW / 2f
                    val gy = zone.clusterCenter.y - glowH / 2f
                    glowP.placeWithLayer(gx.roundToInt(), gy.roundToInt())
                }

                val hudP = placeables["hud-${zone.handIndex}"]
                if (hudP != null) {
                    hudP.placeWithLayer(
                        x = zone.clusterTopLeft.x.roundToInt(),
                        y = zone.clusterTopLeft.y.roundToInt()
                    )
                }
            }
        }
    }
}
