package io.github.smithjustinn.blackjack

import kotlin.test.Test

class BenchmarkTest {
    @Test
    fun runHandBenchmark() {
        val cards = kotlinx.collections.immutable.persistentListOf(
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.TEN, Suit.HEARTS),
            Card(Rank.TWO, Suit.HEARTS, true),
            Card(Rank.ACE, Suit.HEARTS),
            Card(Rank.TWO, Suit.HEARTS)
        )

        // Warmup
        for (i in 0..100000) {
            val faceUpCards = cards.filter { !it.isFaceDown }
            var s = faceUpCards.sumOf { it.rank.value }
            var aces = faceUpCards.count { it.rank == Rank.ACE }
            while (s > 21 && aces > 0) {
                s -= 10
                aces -= 1
            }
        }

        val timeOld = kotlin.system.measureTimeMillis {
            for (i in 0..1000000) {
                val faceUpCards = cards.filter { !it.isFaceDown }
                var s = faceUpCards.sumOf { it.rank.value }
                var aces = faceUpCards.count { it.rank == Rank.ACE }
                while (s > 21 && aces > 0) {
                    s -= 10
                    aces -= 1
                }
            }
        }

        // Warmup
        for (i in 0..100000) {
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

        val timeNew = kotlin.system.measureTimeMillis {
            for (i in 0..1000000) {
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

        println("===============================")
        println("Old approach: $timeOld ms")
        println("New approach: $timeNew ms")
        println("Improvement: ${(timeOld - timeNew).toDouble() / timeOld * 100}%")
        println("===============================")
    }
}
