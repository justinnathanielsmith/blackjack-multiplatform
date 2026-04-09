package io.github.smithjustinn.blackjack.data

import io.github.smithjustinn.blackjack.model.BlackjackConfig
import kotlinx.coroutines.flow.Flow

interface BalanceService {
    val balanceFlow: Flow<Int>

    suspend fun saveBalance(balance: Int)

    suspend fun resetBalance()

    companion object {
        const val DEFAULT_BALANCE = BlackjackConfig.INITIAL_BALANCE
    }
}
