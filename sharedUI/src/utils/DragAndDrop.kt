package io.github.smithjustinn.blackjack.utils

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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

    fun startDrag(
        item: Any,
        sourcePosition: Offset
    ) {
        dragItem = item
        dragItemSourcePosition = sourcePosition
        dragPosition = sourcePosition
        dragOffset = Offset.Zero
        isDragging = true
    }

    fun stopDrag() {
        isDragging = false
        // Keep dragItem until the next drag starts so DropTarget can access it
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
}

@Composable
fun DragTarget(
    modifier: Modifier = Modifier,
    item: Any,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val state = LocalDragAndDropState.current
    var currentPosition by remember { mutableStateOf(Offset.Zero) }

    val dragModifier =
        if (enabled) {
            Modifier.pointerInput(item) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { _ ->
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
        } else {
            Modifier
        }

    Box(
        modifier =
            modifier
                .onGloballyPositioned {
                    currentPosition = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f)
                }.then(dragModifier)
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
    var bounds by remember { mutableStateOf(Rect.Zero) }

    // Use derivedStateOf to avoid unnecessary recompositions
    val isCurrentlyHovered =
        remember(bounds, state.isDragging, state.dragPosition) {
            state.isDragging && bounds.contains(state.dragPosition)
        }

    // Update isHovered state
    SideEffect {
        if (state.isDragging) {
            isHovered = isCurrentlyHovered
        }
    }

    Box(
        modifier =
            modifier
                .onGloballyPositioned {
                    val position = it.positionInRoot()
                    bounds =
                        Rect(
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
    LaunchedEffect(state.isDragging) {
        if (!state.isDragging && isHovered) {
            state.dragItem?.let { onDrop(it) }
            state.clearDragItem()
            isHovered = false
        }
    }
}
