package io.github.smithjustinn.blackjack.ui.components.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * Draws the 3D depth effect of a casino chip: bottom shadow and side cylinder circles.
 *
 * @param chipColor The primary color of the chip.
 * @param radius The radius of the chip face.
 * @param center The center [Offset] of the chip.
 * @param depthOffset The vertical pixel offset that creates the 3D cylinder illusion.
 */
fun DrawScope.drawChipDepth(
    chipColor: Color,
    radius: Float,
    center: Offset,
    depthOffset: Float,
) {
    // Bottom shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.4f),
        radius = radius,
        center = center.copy(y = center.y + depthOffset + 2f),
    )
    // Outer side of the chip cylinder
    drawCircle(
        color = chipColor.copy(alpha = 0.6f),
        radius = radius,
        center = center.copy(y = center.y + depthOffset),
    )
    // Inner side of the chip cylinder (mid-point highlight)
    drawCircle(
        color = chipColor.copy(alpha = 0.7f),
        radius = radius,
        center = center.copy(y = center.y + depthOffset / 2),
    )
}

/**
 * Draws the main chip face: radial surface, rim, clay-spot dashes, and inner recess shadow.
 *
 * @param mainBrush Pre-computed radial gradient brush for the chip surface.
 * @param outerRimStroke Pre-computed [Stroke] for the outer rim highlight.
 * @param dashedStroke Pre-computed [Stroke] for the clay-edge spot dashes.
 * @param innerHighlightStroke Pre-computed [Stroke] for the inner recess shadow ring.
 * @param radius The radius of the chip face.
 * @param center The center [Offset] of the chip.
 */
fun DrawScope.drawChipBody(
    mainBrush: Brush,
    outerRimStroke: Stroke,
    dashedStroke: Stroke,
    innerHighlightStroke: Stroke,
    radius: Float,
    center: Offset,
) {
    // Main top surface
    drawCircle(brush = mainBrush, radius = radius, center = center)

    // Outer rim highlight
    drawCircle(
        color = Color.White.copy(alpha = 0.3f),
        radius = radius - 0.5f,
        center = center,
        style = outerRimStroke,
    )

    // Contrasting clay-edge spot dashes
    drawCircle(
        color = Color.White,
        radius = radius * 0.90f,
        center = center,
        style = dashedStroke,
    )

    // Inner circle recess shadow
    drawCircle(
        color = Color.Black.copy(alpha = 0.25f),
        radius = radius * 0.70f,
        center = center,
        style = innerHighlightStroke,
    )
}

/**
 * Draws the chip's center inlay and top gloss lighting.
 *
 * @param topGlossBrush Pre-computed linear gradient brush for the gloss lighting effect.
 * @param radius The radius of the chip face.
 * @param center The center [Offset] of the chip.
 */
fun DrawScope.drawChipHighlight(
    topGlossBrush: Brush,
    radius: Float,
    center: Offset,
) {
    // Center inlay area (white disc for the denomination label background)
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = radius * 0.65f,
        center = center,
    )

    // Gloss lighting over the entire top surface
    drawCircle(brush = topGlossBrush, radius = radius, center = center)
}
