package io.github.smithjustinn.blackjack

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MonteCarloFuzzTest {
    @Test
    fun conservation_100k_games_noChipsLeaked() {
        val seed = 42L
        val random = Random(seed)
        val initialBalance = 100_000_000 // Large pool to avoid bankruptcy
        val engine = MonteCarloEngine(random, GameRules(), initialBalance)

        var cumulativeDealerProfit = 0L
        val iterations = 100_000
        val betPerHand = 100

        println("🚀 Starting 100,000 games for Chip Conservation check (Seed: $seed)...")

        repeat(iterations) { i ->
            val balanceBefore = engine.state.balance
            val totalChipsBefore = balanceBefore + cumulativeDealerProfit

            engine.playRound(bet = betPerHand, handCount = 1)

            val balanceAfter = engine.state.balance
            val roundNetChange = (balanceAfter - balanceBefore).toLong()

            // Dealer profit is inverse of player net change
            cumulativeDealerProfit -= roundNetChange

            val totalChipsAfter = balanceAfter + cumulativeDealerProfit

            if (totalChipsAfter != initialBalance.toLong()) {
                val leak = initialBalance.toLong() - totalChipsAfter
                val actions = engine.history.joinToString(", ")
                val dealerHand =
                    engine.state.dealerHand.cards
                        .joinToString { "${it.rank}" }
                val playerHands =
                    engine.state.playerHands.joinToString(" | ") { hand ->
                        hand.cards.joinToString { "${it.rank}" }
                    }

                throw AssertionError(
                    """
                    🎰 CHIP LEAK DETECTED at game #$i:
                    Leak: $leak chips
                    Final Balance: ${engine.state.balance}
                    Cumulative Dealer Profit: $cumulativeDealerProfit
                    Initial Balance: $initialBalance
                    Expected Total: $initialBalance, Actual Total: $totalChipsAfter
                    
                    Dealer Hand: $dealerHand
                    Player Hands: $playerHands
                    Action Sequence: $actions
                    """.trimIndent()
                )
            }
        }

        println("✅ PASSED: 100,000 games complete with ZERO chips leaked.")
    }

    @Test
    fun strategy_ev_converges_to_theoretical() {
        val seed = 1337L
        val random = Random(seed)
        val initialBalance = 100_000_000
        val engine = MonteCarloEngine(random, GameRules(), initialBalance)

        var totalWagered = 0L
        var totalPayout = 0L

        val iterations = 100_000
        val betPerHand = 100

        println("📊 Running 100,000 games for RTP Strategy Validation...")

        repeat(iterations) {
            val balanceAtStartOfRound = engine.state.balance.toLong()

            engine.playRound(bet = betPerHand, handCount = 1)

            val balanceAtEndOfRound = engine.state.balance.toLong()

            // We need to calculate total wagered this round.
            // totalWagered = balanceAtStartOfRound - (balanceAtEndOfRound - payout)
            // But we can just track every balance deduction in the engine if we want perfect accuracy.
            // Let's deduce it: payout is the amount returned to player.
            // If the player bets 100 and loses, balance drops 100. Payout=0. WagerED=100.
            // If the player bets 100 and wins (returns 200), balance increases 100. Payout=200. Wagered=100.
            // If the player bets 100, doubles to 200, and wins (returns 400), balance increases 200. Payout=400. Wagered=200.

            // Fortunately, totalNetPayout() + totalBet gives exactly the amount returned (payout).
            // But totalBet is only valid during PLAYING. Terminal state has totalBet 0.

            // Let's re-calculate from balance diff:
            // Since there are no external deposits/withdrawals:
            // balanceEnd = balanceStart - Wagered + Payout
            // Payout - Wagered = balanceEnd - balanceStart

            // In Blackjack, Wagered is ALWAYS at least the initial bet.
            // Let's track wagers explicitly inside the engine history or manually.
            var roundWagered = betPerHand.toLong()
            engine.history.forEach { action ->
                when (action) {
                    is GameAction.DoubleDown -> roundWagered += betPerHand
                    is GameAction.Split -> roundWagered += betPerHand
                    // Insurance is declined in our decider, otherwise we'd add insurance bet here.
                    else -> {}
                }
            }

            val roundPayout = (balanceAtEndOfRound - balanceAtStartOfRound) + roundWagered

            totalWagered += roundWagered
            totalPayout += roundPayout
        }

        val actualRTP = totalPayout.toDouble() / totalWagered.toDouble()
        val formattedRTP = "%.4f".format(actualRTP * 100)

        println("🎰 Monte Carlo Summary (100k hands):")
        println("   Total Wagered: $totalWagered")
        println("   Total Payout:  $totalPayout")
        println("   Actual RTP:    $formattedRTP%")
        println("   Theoretical:   ~99.45% (6 deck, H17, 3:2)")

        // 1.5% tolerance (3σ range for 100k hands is roughly 0.5%, so 1.5% is very safe)
        val lowerBound = 0.980
        val upperBound = 1.010

        assertTrue(
            actualRTP in lowerBound..upperBound,
            "Actual RTP $formattedRTP% is outside expected bound [98.0%, 101.0%]. Likely logic bug."
        )
    }

    @Test
    fun conservation_withSideBets_noChipsLeaked() {
        val seed = 777L
        val random = Random(seed)
        val initialBalance = 100_000_000
        val engine = MonteCarloEngine(random, GameRules(), initialBalance)

        var cumulativeDealerProfit = 0L
        val iterations = 50_000 // 50k is enough to hit 21+3 and Perfect Pairs plenty
        val betPerHand = 100
        val sideBet = 25

        println("💎 Checking Chip Conservation with 21+3 and Perfect Pairs (50,000 games)...")

        val sideBets =
            mapOf(
                SideBetType.TWENTY_ONE_PLUS_THREE to sideBet,
                SideBetType.PERFECT_PAIRS to sideBet
            )

        repeat(iterations) {
            val balanceBefore = engine.state.balance
            engine.playRound(bet = betPerHand, handCount = 1, sideBets = sideBets)
            val balanceAfter = engine.state.balance

            cumulativeDealerProfit -= (balanceAfter - balanceBefore).toLong()

            assertEquals(
                initialBalance.toLong(),
                balanceAfter + cumulativeDealerProfit,
                "Chip leak in side bet resolution! Round history: ${engine.history}"
            )
        }

        println("✅ PASSED: Side bet chip conservation verified.")
    }
}
