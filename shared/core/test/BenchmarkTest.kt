package io.github.smithjustinn.blackjack

import kotlin.test.Test

class BenchmarkTest {
    @Test
    fun runHandBenchmark() {
        val cards =
            kotlinx.collections.immutable.persistentListOf(
                Card(Rank.ACE, Suit.HEARTS),
                Card(Rank.TEN, Suit.HEARTS),
                Card(Rank.TWO, Suit.HEARTS, true),
                Card(Rank.ACE, Suit.HEARTS),
                Card(Rank.TWO, Suit.HEARTS)
            )

        // Warmup old
        repeat(100_000) { runOldApproach(cards) }

        val timeOld =
            kotlin.system.measureTimeMillis {
                repeat(1_000_000) { runOldApproach(cards) }
            }

        // Warmup new
        repeat(100_000) { runNewApproach(cards) }

        val timeNew =
            kotlin.system.measureTimeMillis {
                repeat(1_000_000) { runNewApproach(cards) }
            }

        println("===============================")
        println("Old approach: $timeOld ms")
        println("New approach: $timeNew ms")
        if (timeOld > 0) {
            println("Improvement: ${(timeOld - timeNew).toDouble() / timeOld * 100}%")
        }
        println("===============================")
    }

    private fun runOldApproach(cards: List<Card>) {
        val faceUpCards = cards.filter { !it.isFaceDown }
        var s = faceUpCards.sumOf { it.rank.value }
        var aces = faceUpCards.count { it.rank == Rank.ACE }
        while (s > 21 && aces > 0) {
            s -= 10
            aces -= 1
        }
    }

    private fun runNewApproach(cards: List<Card>) {
        var s = 0
        var aces = 0
        for (card in cards) {
            if (!card.isFaceDown) {
                s += card.rank.value
                if (card.rank == Rank.ACE) {
                    aces++
                }
            }
        }
        while (s > 21 && aces > 0) {
            s -= 10
            aces -= 1
        }
    }
}
