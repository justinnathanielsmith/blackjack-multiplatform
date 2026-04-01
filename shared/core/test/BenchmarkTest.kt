package io.github.smithjustinn.blackjack

import Random
import measureTimeMillis
import kotlin.test.Test
import persistentListOf

class BenchmarkTest {
    @Test
    fun runHandBenchmark() {
        val cards =
            persistentListOf(
                Card(Rank.ACE, Suit.HEARTS),
                Card(Rank.TEN, Suit.HEARTS),
                Card(Rank.TWO, Suit.HEARTS, true),
                Card(Rank.ACE, Suit.HEARTS),
                Card(Rank.TWO, Suit.HEARTS)
            )

        // Warmup old
        repeat(100_000) { runOldApproach(cards) }

        val timeOld =
            measureTimeMillis {
                repeat(1_000_000) { runOldApproach(cards) }
            }

        // Warmup new
        repeat(100_000) { runNewApproach(cards) }

        val timeNew =
            measureTimeMillis {
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

    @Test
    fun benchmarkCreateDeck() {
        val rules = GameRules(deckCount = 6)
        val random = Random(123)

        // Run benchmarks multiple times to get a stable result
        println("===============================")
        var totalOld = 0L
        var totalNew = 0L
        val iterations = 5

        for (i in 1..iterations) {
            // Warmup
            repeat(10_000) { createDeckOld(rules, random) }
            val timeOld =
                measureTimeMillis {
                    repeat(200_000) { createDeckOld(rules, random) }
                }

            // Warmup
            repeat(10_000) { createDeckNew(rules, random) }
            val timeNew =
                measureTimeMillis {
                    repeat(200_000) { createDeckNew(rules, random) }
                }

            totalOld += timeOld
            totalNew += timeNew
            println("Iteration $i - Old: $timeOld ms, New: $timeNew ms")
        }

        println("Deck Average Old: ${totalOld / iterations} ms")
        println("Deck Average New: ${totalNew / iterations} ms")
        if (totalOld > 0) {
            println("Average Improvement: ${(totalOld - totalNew).toDouble() / totalOld * 100}%")
        }
        println("===============================")
    }

    private fun createDeckOld(
        rules: GameRules,
        random: Random
    ): List<Card> {
        val deckSize = rules.deckCount * BlackjackRules.CARDS_PER_DECK
        val newDeck = ArrayList<Card>(deckSize)
        for (i in 1..rules.deckCount) {
            for (suit in Suit.entries) {
                for (rank in Rank.entries) {
                    newDeck.add(Card(rank, suit))
                }
            }
        }
        newDeck.shuffle(random)
        return newDeck
    }

    private fun createDeckNew(
        rules: GameRules,
        random: Random
    ): List<Card> {
        val deckSize = rules.deckCount * BlackjackRules.CARDS_PER_DECK
        val newDeck = ArrayList<Card>(deckSize)
        val suits = Suit.entries
        val ranks = Rank.entries
        for (i in 1..rules.deckCount) {
            for (i in 0 until suits.size) {
                val suit = suits[i]
                for (j in 0 until ranks.size) {
                    newDeck.add(Card(ranks[j], suit))
                }
            }
        }
        newDeck.shuffle(random)
        return newDeck
    }
}
