package io.github.smithjustinn.blackjack.logic
import io.github.smithjustinn.blackjack.action.*
import io.github.smithjustinn.blackjack.infra.*
import io.github.smithjustinn.blackjack.logic.*
import io.github.smithjustinn.blackjack.middleware.*
import io.github.smithjustinn.blackjack.model.*
import io.github.smithjustinn.blackjack.state.*
import io.github.smithjustinn.blackjack.util.*
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class DeckGenerationTest {
    @Test
    fun createDeck_yieldsCorrectCardCounts() {
        val rules = GameRules(deckCount = 6)
        val deck = BlackjackRules.createDeck(rules, Random(42))

        // 6 decks * 52 cards = 312
        assertEquals(312, deck.size)

        // Each Rank-Suit combination should appear exactly deckCount times
        val counts = deck.groupBy { it.rank to it.suit }.mapValues { it.value.size }

        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                assertEquals(6, counts[rank to suit], "Count for $rank of $suit is wrong")
            }
        }
    }

    @Test
    fun createDeck_randomizesOrder() {
        val rules = GameRules(deckCount = 6)
        // Two decks with different seeds should be different
        val deck1 = BlackjackRules.createDeck(rules, Random(1))
        val deck2 = BlackjackRules.createDeck(rules, Random(2))

        assertNotEquals(deck1, deck2)
    }

    @Test
    fun createDeck_withDifferentShoeSize() {
        val rules = GameRules(deckCount = 1)
        val deck = BlackjackRules.createDeck(rules, Random(42))
        assertEquals(52, deck.size)

        val counts = deck.groupBy { it.rank to it.suit }.mapValues { it.value.size }
        for (rank in Rank.entries) {
            for (suit in Suit.entries) {
                assertEquals(1, counts[rank to suit])
            }
        }
    }
}
