package io.github.smithjustinn.blackjack.ui.effects

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import io.github.smithjustinn.blackjack.ui.components.TableLayout

class DealAnimationRegistry {
    var overlayOffset by mutableStateOf(Offset.Zero)
    var gameplayAreaOffset by mutableStateOf(Offset.Zero)
    var shoePosition by mutableStateOf(Offset.Zero)
    var tableLayout by mutableStateOf<TableLayout?>(null)
}

val LocalDealAnimationRegistry = staticCompositionLocalOf { DealAnimationRegistry() }
