package io.github.smithjustinn.blackjack.services

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceServiceTest {
    class FakeDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow<Preferences>(emptyPreferences())

        override val data: Flow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            state.update {
                transform(it)
            }
            return state.value
        }

        fun emitPreferences(prefs: Preferences) {
            state.value = prefs
        }
    }

    class ExceptionFakeDataStore : DataStore<Preferences> {
        override val data: Flow<Preferences> =
            flow {
                throw RuntimeException("Datastore error")
            }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
            return emptyPreferences()
        }
    }

    @Test
    fun testDefaultBalanceIsEmittedWhenEmpty() =
        runTest {
            val dataStore = FakeDataStore()
            val service = BalanceService(dataStore)

            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }

    @Test
    fun testSaveBalanceUpdatesValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = BalanceService(dataStore)

            service.saveBalance(2500)

            assertEquals(2500, service.balanceFlow.first())
        }

    @Test
    fun testResetBalanceRestoresDefaultValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = BalanceService(dataStore)

            service.saveBalance(500)
            assertEquals(500, service.balanceFlow.first())

            service.resetBalance()
            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }

    @Test
    fun testExceptionInDataFlowReturnsDefaultValue() =
        runTest {
            val dataStore = ExceptionFakeDataStore()
            val service = BalanceService(dataStore)

            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }
}
