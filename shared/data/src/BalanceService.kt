package io.github.smithjustinn.blackjack.data

import kotlinx.coroutines.flow.Flow

interface BalanceService {
    val balanceFlow: Flow<Int>

    suspend fun saveBalance(balance: Int)

    suspend fun resetBalance()

    companion object {
        const val DEFAULT_BALANCE = 1000
    }
}
