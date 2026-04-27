import io.github.smithjustinn.blackjack.action.GameAction
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle

fun main() = runTest {
    val sm = testMachine(playingState(
        playerHand = hand(Rank.TEN, Rank.SEVEN),
        dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
        rules = GameRules(allowSurrender = true)
    ))
    println("Dispatching Surrender")
    sm.dispatch(GameAction.Surrender)
    println("Advancing until idle")
    advanceUntilIdle()
    println("Status: ${sm.state.value.status}")
}
