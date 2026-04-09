package io.github.smithjustinn.blackjack.benchmarking
import co.touchlab.kermit.Logger
import io.github.smithjustinn.blackjack.logic.BlackjackRules
import io.github.smithjustinn.blackjack.logic.GameRules
import io.github.smithjustinn.blackjack.model.Card
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import kotlinx.collections.immutable.persistentListOf
import kotlin.collections.ArrayList
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test

class BenchmarkTest {
    private val logger = Logger.withTag("BenchmarkTest")

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

        logger.i { "===============================" }
        logger.i { "Old approach: $timeOld ms" }
        logger.i { "New approach: $timeNew ms" }
        if (timeOld > 0) {
            logger.i { "Improvement: ${(timeOld - timeNew).toDouble() / timeOld * 100}%" }
        }
        logger.i { "===============================" }
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
        logger.i { "===============================" }
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
            logger.i { "Iteration $i - Old: $timeOld ms, New: $timeNew ms" }
        }

        logger.i { "Deck Average Old: ${totalOld / iterations} ms" }
        logger.i { "Deck Average New: ${totalNew / iterations} ms" }
        if (totalOld > 0) {
            logger.i { "Average Improvement: ${(totalOld - totalNew).toDouble() / totalOld * 100}%" }
        }
        logger.i { "===============================" }
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
