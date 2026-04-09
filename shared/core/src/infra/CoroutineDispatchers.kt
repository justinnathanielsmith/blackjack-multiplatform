package io.github.smithjustinn.blackjack.infra

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// Dispatchers.IO is common-API since coroutines 1.7 — no platform actuals needed.
val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

data class CoroutineDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    val io: CoroutineDispatcher = ioDispatcher,
    val default: CoroutineDispatcher = Dispatchers.Default,
)
