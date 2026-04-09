package io.github.smithjustinn.blackjack.di

import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.data.BalanceService
import io.github.smithjustinn.blackjack.data.SettingsRepository
import io.github.smithjustinn.blackjack.data.createBalanceService
import io.github.smithjustinn.blackjack.data.createSettingsRepository
import io.github.smithjustinn.blackjack.infra.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.dsl.module

// Platform-agnostic bindings shared across Android, Desktop, and iOS
val commonModule =
    module {
        single<CoroutineDispatchers> { CoroutineDispatchers() }
        single<CoroutineScope> { MainScope() }
        single<Logger> { Logger.withTag("Blackjack") }
        single<BalanceService> { createBalanceService() }
        single<SettingsRepository> { createSettingsRepository() }
    }
