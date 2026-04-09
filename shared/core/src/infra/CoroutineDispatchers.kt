package io.github.smithjustinn.blackjack.infra
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
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
