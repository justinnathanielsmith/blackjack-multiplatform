package io.github.smithjustinn.blackjack.di

import androidx.compose.runtime.staticCompositionLocalOf
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope

interface AppGraph {
    val logger: Logger
    val coroutineDispatchers: CoroutineDispatchers
    val applicationScope: CoroutineScope
}

val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("No AppGraph provided")
}
