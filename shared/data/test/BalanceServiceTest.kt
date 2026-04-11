package io.github.smithjustinn.blackjack.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceServiceTest {
    @Test
    fun testDefaultBalanceIsEmittedWhenEmpty() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }

    @Test
    fun testSaveBalanceUpdatesValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.saveBalance(2500)

            assertEquals(2500, service.balanceFlow.first())
        }

    @Test
    fun testResetBalanceRestoresDefaultValue() =
        runTest {
            val dataStore = FakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            service.saveBalance(500)
            assertEquals(500, service.balanceFlow.first())

            service.resetBalance()
            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }

    @Test
    fun testExceptionInDataFlowReturnsDefaultValue() =
        runTest {
            val dataStore = ExceptionFakeDataStore()
            val service = DataStoreBalanceService(dataStore)

            assertEquals(BalanceService.DEFAULT_BALANCE, service.balanceFlow.first())
        }
}
