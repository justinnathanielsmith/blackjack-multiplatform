package io.github.smithjustinn.blackjack.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

expect val ioDispatcher: CoroutineDispatcher

data class CoroutineDispatchers(
    val main: CoroutineDispatcher = Dispatchers.Main,
    val mainImmediate: CoroutineDispatcher = Dispatchers.Main.immediate,
    val io: CoroutineDispatcher = ioDispatcher,
    val default: CoroutineDispatcher = Dispatchers.Default,
)
