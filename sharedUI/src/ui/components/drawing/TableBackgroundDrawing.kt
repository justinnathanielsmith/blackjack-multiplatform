package io.github.smithjustinn.blackjack.ui.components.drawing

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.smithjustinn.blackjack.ui.theme.FeltDeepEdge
import io.github.smithjustinn.blackjack.ui.theme.OakMedium

/**
 * Draws the radial felt gradient that forms the base of the blackjack table surface.
 *
 * @param feltBrush Pre-computed radial gradient brush — recreated only on size change via [drawWithCache].
 */
fun DrawScope.drawFeltGradient(feltBrush: Brush) {
    drawRect(brush = feltBrush)
}

/**
 * Draws subtle linen fiber texture over the felt using vertical and horizontal hairlines.
 * Alpha is intentionally very low (0.05) to keep the effect subliminal.
 */
fun DrawScope.drawFiberTexture() {
    val fiberSpacing = 2.dp.toPx()
    val fiberAlpha = 0.05f
    val fiberStrokeWidth = 0.5.dp.toPx()
    for (x in 0..(size.width / fiberSpacing).toInt()) {
        drawLine(
            color = Color.Black.copy(alpha = fiberAlpha),
            start = Offset(x * fiberSpacing, 0f),
            end = Offset(x * fiberSpacing, size.height),
            strokeWidth = fiberStrokeWidth,
        )
    }
    for (y in 0..(size.height / fiberSpacing).toInt()) {
        drawLine(
            color = Color.Black.copy(alpha = fiberAlpha),
            start = Offset(0f, y * fiberSpacing),
            end = Offset(size.width, y * fiberSpacing),
            strokeWidth = fiberStrokeWidth,
        )
    }
}

/**
 * Draws the classic table arc (betting line) and the insurance arc above it, both with
 * an embossed shadow pass followed by a gold highlight pass.
 *
 * @param arcLeft X coordinate of the arc bounding box left edge.
 * @param arcTop Y coordinate of the arc bounding box top edge (betting arc).
 * @param arcSize Size of the arc bounding box.
 * @param arcStroke Pre-computed [Stroke] for the main betting arc.
 * @param insuranceStroke Pre-computed [Stroke] for the thinner insurance arc.
 * @param insuranceOffset Vertical pixel distance from the betting arc to the insurance arc.
 * @param primaryGold The gold color used for the arc highlights.
 */
fun DrawScope.drawBettingArc(
    arcLeft: Float,
    arcTop: Float,
    arcSize: Size,
    arcStroke: Stroke,
    insuranceStroke: Stroke,
    insuranceOffset: Float,
    primaryGold: Color,
) {
    // Main betting arc — shadow pass
    drawArc(
        color = Color.Black.copy(alpha = 0.25f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(arcLeft, arcTop + 1.dp.toPx()),
        size = arcSize,
        style = arcStroke,
    )
    // Main betting arc — gold highlight pass
    drawArc(
        color = primaryGold.copy(alpha = 0.15f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(arcLeft, arcTop),
        size = arcSize,
        style = arcStroke,
    )

    drawInsuranceArc(arcLeft, arcTop, arcSize, insuranceStroke, insuranceOffset, primaryGold)
    drawStitchedDashLine(arcLeft, arcTop, arcSize, insuranceOffset)
}

private fun DrawScope.drawInsuranceArc(
    arcLeft: Float,
    arcTop: Float,
    arcSize: Size,
    insuranceStroke: Stroke,
    insuranceOffset: Float,
    primaryGold: Color,
) {
    // Insurance arc — shadow pass
    drawArc(
        color = Color.Black.copy(alpha = 0.2f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(arcLeft, (arcTop - insuranceOffset) + 1.dp.toPx()),
        size = arcSize,
        style = insuranceStroke,
    )
    // Insurance arc — gold highlight pass
    drawArc(
        color = primaryGold.copy(alpha = 0.08f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(arcLeft, arcTop - insuranceOffset),
        size = arcSize,
        style = insuranceStroke,
    )
}

private fun DrawScope.drawStitchedDashLine(
    arcLeft: Float,
    arcTop: Float,
    arcSize: Size,
    insuranceOffset: Float,
) {
    // Stitched Dash Line Effect
    val dashPath =
        androidx.compose.ui.graphics.Path().apply {
            addArc(
                androidx.compose.ui.geometry.Rect(
                    topLeft = Offset(arcLeft, arcTop - insuranceOffset / 2f),
                    bottomRight = Offset(arcLeft + arcSize.width, arcTop + arcSize.height - insuranceOffset / 2f)
                ),
                180f,
                180f
            )
        }
    drawPath(
        path = dashPath,
        color = Color.White.copy(alpha = 0.12f),
        style =
            Stroke(
                width = 1.dp.toPx(),
                pathEffect =
                    androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8.dp.toPx(), 8.dp.toPx()),
                        phase = 0f
                    )
            )
    )
}

/**
 * Draws the heavy radial vignette that simulates the dark leather rail around the table
 * and gives the "high roller" mood.
 *
 * @param vignetteBrush Pre-computed radial gradient brush — recreated only on size change via [drawWithCache].
 */
fun DrawScope.drawVignette(vignetteBrush: Brush) {
    drawRect(brush = vignetteBrush)
    // Deepen the corners for a more focused mood
    drawRect(
        brush =
            Brush.radialGradient(
                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                radius = size.maxDimension / 2f
            )
    )
}

/**
 * Draws the physical wood table rails at the top and bottom of the screen with highlight lines.
 *
 * @param railHeight Height of each rail in pixels.
 */
fun DrawScope.drawRails(railHeight: Float) {
    val highlightStrokeWidth = 1.dp.toPx()

    // Bottom rail — felt-to-oak gradient
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(OakMedium, FeltDeepEdge),
                startY = size.height - railHeight,
                endY = size.height,
            ),
        topLeft = Offset(0f, size.height - railHeight),
        size = Size(size.width, railHeight),
    )
    // Top rail — oak-to-felt gradient
    drawRect(
        brush =
            Brush.verticalGradient(
                colors = listOf(FeltDeepEdge, OakMedium),
                startY = 0f,
                endY = railHeight,
            ),
        topLeft = Offset.Zero,
        size = Size(size.width, railHeight),
    )

    // Rail highlight lines
    drawLine(
        color = Color.White.copy(alpha = 0.1f),
        start = Offset(0f, railHeight),
        end = Offset(size.width, railHeight),
        strokeWidth = highlightStrokeWidth,
    )
    drawLine(
        color = Color.White.copy(alpha = 0.1f),
        start = Offset(0f, size.height - railHeight),
        end = Offset(size.width, size.height - railHeight),
        strokeWidth = highlightStrokeWidth,
    )
}
