package io.github.smithjustinn.blackjack.data.local

import io.github.smithjustinn.blackjack.domain.BalanceService

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceServiceTest {
    @Test
    fun testDefaultBalanceIsEmittedWhenEmpty() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.balanceFlow.test {
                assertEquals(BalanceService.DEFAULT_BALANCE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testSaveBalanceUpdatesValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.balanceFlow.test {
                assertEquals(BalanceService.DEFAULT_BALANCE, awaitItem())
                service.saveBalance(2500)
                assertEquals(2500, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testResetBalanceRestoresDefaultValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.balanceFlow.test {
                assertEquals(BalanceService.DEFAULT_BALANCE, awaitItem())
                service.saveBalance(500)
                assertEquals(500, awaitItem())

                service.resetBalance()
                assertEquals(BalanceService.DEFAULT_BALANCE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun testExceptionInDataFlowReturnsDefaultValue() =
        runTest {
            val dataStore = ExceptionFakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.balanceFlow.test {
                assertEquals(BalanceService.DEFAULT_BALANCE, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
