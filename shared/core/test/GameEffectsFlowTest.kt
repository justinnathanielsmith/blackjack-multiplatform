@file:OptIn(ExperimentalCoroutinesApi::class)

package io.github.smithjustinn.blackjack

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameEffectsFlowTest {

    @Test
    fun hitEmitsPlayCardSound() = runTest {
        // Player EIGHT+THREE=11, draws FIVE → 16 (no bust, game continues)
        val sm = BlackjackStateMachine(
            this,
            playingState(
                playerHand = hand(Rank.EIGHT, Rank.THREE),
                dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                deck = deckOf(Rank.FIVE),
            ),
        )

        sm.effects.test {
            sm.dispatch(GameAction.Hit)
            assertEquals(GameEffect.PlayCardSound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playerWinEmitsPlayWinSound() = runTest {
        // Player TEN+KING=20 stands, dealer TEN+SEVEN=17 stands (no dealer draw)
        val sm = BlackjackStateMachine(
            this,
            playingState(
                playerHand = hand(Rank.TEN, Rank.KING),
                dealerHand = hand(Rank.TEN, Rank.SEVEN),
            ),
        )

        sm.effects.test {
            sm.dispatch(GameAction.Stand)
            assertEquals(GameEffect.PlayWinSound, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun playerBustEmitsPlayCardSoundThenLoseSoundAndVibrate() = runTest {
        // Player TEN+TEN=20, draws TEN → 30 (guaranteed bust)
        val sm = BlackjackStateMachine(
            this,
            playingState(
                playerHand = hand(Rank.TEN, Rank.TEN),
                dealerHand = dealerHand(Rank.TEN, Rank.SEVEN),
                deck = deckOf(Rank.TEN),
            ),
        )

        sm.effects.test {
            sm.dispatch(GameAction.Hit)
            // First 3 emissions from handleHit: PlayCardSound, PlayLoseSound, Vibrate
            assertEquals(GameEffect.PlayCardSound, awaitItem())
            assertEquals(GameEffect.PlayLoseSound, awaitItem())
            assertEquals(GameEffect.Vibrate, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun dealerWinEmitsPlayLoseSoundAndVibrate() = runTest {
        // Player TEN+SIX=16 stands, dealer TEN+NINE=19 wins
        val sm = BlackjackStateMachine(
            this,
            playingState(
                playerHand = hand(Rank.TEN, Rank.SIX),
                dealerHand = hand(Rank.TEN, Rank.NINE),
            ),
        )

        sm.effects.test {
            sm.dispatch(GameAction.Stand)
            val emitted = buildList { repeat(2) { add(awaitItem()) } }
            assertTrue(GameEffect.PlayLoseSound in emitted)
            assertTrue(GameEffect.Vibrate in emitted)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
