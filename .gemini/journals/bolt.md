## 2026-03-29 - Recomposition Trap: animateOffsetAsState with 'by'
**Learning:** Using the delegated 'by' property with animateOffsetAsState in a high-level Screen component causes the entire screen to recompose on every animation frame. This is extremely expensive if the screen has complex drawing or many children.
**Action:** Use 'val state = animateOffsetAsState(...)' and read 'state.value' only inside the narrowest possible scope, preferably a draw scope (drawBehind/onDrawBehind) or a leaf Composable, to bypass high-level recomposition.
