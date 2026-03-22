package io.github.smithjustinn.blackjack.utils

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

internal val LocalDragAndDropState = compositionLocalOf { DragAndDropState() }

class DragAndDropState {
    var isDragging: Boolean by mutableStateOf(false)
    var dragOffset: Offset by mutableStateOf(Offset.Zero)
    var dragPosition: Offset by mutableStateOf(Offset.Zero)
    var dragItem: Any? by mutableStateOf(null)
    var dragItemSourcePosition: Offset by mutableStateOf(Offset.Zero)

    fun startDrag(item: Any, sourcePosition: Offset) {
        dragItem = item
        dragItemSourcePosition = sourcePosition
        dragPosition = sourcePosition
        dragOffset = Offset.Zero
        isDragging = true
    }

    fun stopDrag() {
        isDragging = false
        // Don't clear dragItem immediately so DropTarget can see it
    }

    fun clearDragItem() {
        dragItem = null
    }

    fun updateDrag(offset: Offset) {
        dragOffset += offset
        dragPosition = dragItemSourcePosition + dragOffset
    }
}

@Composable
fun DragAndDropContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = remember { DragAndDropState() }

    CompositionLocalProvider(LocalDragAndDropState provides state) {
        Box(modifier = modifier.fillMaxSize()) {
            content()
        }
    }

    // Clear dragItem after all drop targets have had a chance to react
    androidx.compose.runtime.LaunchedEffect(state.isDragging) {
        if (!state.isDragging) {
            state.clearDragItem()
        }
    }
}

@Composable
fun DragTarget(
    modifier: Modifier = Modifier,
    item: Any,
    content: @Composable () -> Unit
) {
    val state = LocalDragAndDropState.current
    var currentPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                currentPosition = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
            }
            .pointerInput(item) {
                detectDragGestures(
                    onDragStart = {
                        state.startDrag(item, currentPosition)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.updateDrag(dragAmount)
                    },
                    onDragEnd = {
                        state.stopDrag()
                    },
                    onDragCancel = {
                        state.stopDrag()
                    }
                )
            }
    ) {
        content()
    }
}

@Composable
fun DropTarget(
    modifier: Modifier = Modifier,
    onDrop: (Any) -> Unit,
    content: @Composable (isHovered: Boolean) -> Unit
) {
    val state = LocalDragAndDropState.current
    var isHovered by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    // Use derivedStateOf to avoid unnecessary recompositions
    val isCurrentlyHovered = remember(bounds, state.isDragging, state.dragPosition) {
        state.isDragging && bounds.contains(state.dragPosition)
    }

    // Update isHovered state
    androidx.compose.runtime.SideEffect {
        if (state.isDragging) {
            isHovered = isCurrentlyHovered
        }
    }

    Box(
        modifier = modifier
            .onGloballyPositioned {
                val position = it.positionInRoot()
                bounds = androidx.compose.ui.geometry.Rect(
                    position.x,
                    position.y,
                    position.x + it.size.width,
                    position.y + it.size.height
                )
            }
    ) {
        content(isHovered)
    }

    // Effect to handle drop when dragging ends while hovering
    androidx.compose.runtime.LaunchedEffect(state.isDragging) {
        if (!state.isDragging && isHovered) {
            state.dragItem?.let { onDrop(it) }
            isHovered = false
        }
    }
}
