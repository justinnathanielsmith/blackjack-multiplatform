package io.github.smithjustinn.blackjack.di

import io.github.smithjustinn.blackjack.utils.CoroutineDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.koin.dsl.module

val commonModule = module {
    single<CoroutineDispatchers> { CoroutineDispatchers() }
    single<CoroutineScope> { MainScope() }
}
