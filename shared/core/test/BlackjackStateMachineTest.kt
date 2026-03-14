package io.github.smithjustinn.blackjack

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BlackjackStateMachineTest {
    @Test
    fun testPlaceBet_decreasesBalanceAndIncreasesCurrentBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun testPlaceBet_multipleChipsAccumulate() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(850, state.balance)
            assertEquals(150, state.currentBet)
        }

    @Test
    fun testPlaceBet_rejectedWhenAmountExceedsBalance() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 100, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(200))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(100, state.balance)
            assertEquals(0, state.currentBet)
        }

    @Test
    fun testPlaceBet_rejectedWhenAmountZeroOrNegative() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.PlaceBet(0))
            advanceUntilIdle()
            assertEquals(1000, stateMachine.state.value.balance)
            assertEquals(0, stateMachine.state.value.currentBet)

            stateMachine.dispatch(GameAction.PlaceBet(-50))
            advanceUntilIdle()
            assertEquals(1000, stateMachine.state.value.balance)
            assertEquals(0, stateMachine.state.value.currentBet)
        }

    @Test
    fun testPlaceBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 1000, currentBet = 100))
            stateMachine.dispatch(GameAction.PlaceBet(50))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(100, state.currentBet)
        }

    @Test
    fun testResetBet_restoresBalanceAndClearsCurrentBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100))
            stateMachine.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(0, state.currentBet)
            assertEquals(GameStatus.BETTING, state.status)
        }

    @Test
    fun testResetBet_ignoredWhenNotInBettingPhase() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.PLAYING, balance = 900, currentBet = 100))
            stateMachine.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(900, state.balance)
            assertEquals(100, state.currentBet)
        }

    @Test
    fun testDeal_ignoredWhenNoBet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.BETTING, state.status)
            assertEquals(0, state.playerHand.cards.size)
            assertEquals(0, state.dealerHand.cards.size)
        }

    @Test
    fun testInitialDeal() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHand.cards.size)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(48, state.deck.size)
        }

    @Test
    fun testPlayerHit() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Skip test if initial deal resulted in blackjack (game already over)
            if (stateMachine.state.value.status != GameStatus.PLAYING) return@runTest

            val initialPlayerCards = stateMachine.state.value.playerHand.cards.size
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(initialPlayerCards + 1, stateMachine.state.value.playerHand.cards.size)
            assertEquals(47, stateMachine.state.value.deck.size)
        }

    @Test
    fun testBust() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            // Hit until bust
            while (stateMachine.state.value.status == GameStatus.PLAYING) {
                stateMachine.dispatch(GameAction.Hit)
                advanceUntilIdle()
            }

            if (stateMachine.state.value.playerHand.isBust) {
                assertEquals(GameStatus.DEALER_WON, stateMachine.state.value.status)
            }
        }

    @Test
    fun testPlayerWin_afterStand_rewardsBalance() =
        runTest {
            // Player: TEN + KING = 20, Dealer: TEN + SEVEN = 17 (won't draw, deck empty)
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.KING, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1100, state.balance) // 900 + 100 * 2 (bet returned + equal winnings)
        }

    @Test
    fun testPush_afterStand_returnsBet() =
        runTest {
            // Player: TEN + NINE = 19, Dealer: TEN + NINE = 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance) // 900 + 100 (bet returned)
        }

    @Test
    fun testDeal_dealerSecondCardIsFaceDown_orRevealedOnNaturalBlackjack() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.PlaceBet(100))
            advanceUntilIdle()
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            when (state.status) {
                // Dealer natural BJ or push: hole card is revealed
                GameStatus.PUSH, GameStatus.DEALER_WON ->
                    assertFalse(state.dealerHand.cards[1].isFaceDown)
                // Player natural BJ (no dealer BJ) or normal play: hole card stays hidden
                GameStatus.PLAYING, GameStatus.PLAYER_WON ->
                    assertTrue(state.dealerHand.cards[1].isFaceDown)
                else -> {}
            }
        }

    @Test
    fun testStand_revealsHoleCardBeforeDealerDraws() =
        runTest {
            // Player: TEN + SIX = 16, Dealer: TEN (face-up) + NINE (face-down) = 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.NINE, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            state.dealerHand.cards.forEach { card ->
                assertFalse(card.isFaceDown)
            }
        }

    @Test
    fun testBust_holeCardRemainsHidden() =
        runTest {
            // Player: TEN + TEN = 20 then hits a bust-causing card
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.NINE, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = listOf(Card(Rank.FIVE, Suit.SPADES)) // Player draws 5 → 25 bust
                    )
                )
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            // Dealer never reveals when player busts
            assertTrue(state.dealerHand.cards[1].isFaceDown)
        }

    @Test
    fun testDealerWin_afterStand_keepsBalanceUnchanged() =
        runTest {
            // Player: TEN + SIX = 16, Dealer: TEN + NINE = 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(900, state.balance) // unchanged, bet is lost
        }

    // --- Double Down ---

    @Test
    fun doubleDown_doublesbet_and_deals_one_card() =
        runTest {
            // Player: FIVE + SIX = 11, next card is TWO → 13, dealer won't bust
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = listOf(Card(Rank.TWO, Suit.SPADES))
                    )
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(200, state.currentBet)
            assertEquals(800, state.balance + state.currentBet - state.currentBet) // balance already adjusted
            assertEquals(3, state.playerHand.cards.size)
            assertEquals(0, state.deck.size)
        }

    @Test
    fun doubleDown_transitions_to_dealer_turn() =
        runTest {
            // Player: FIVE + SIX = 11, draws TWO → 13 (no bust), dealer: TEN + SEVEN = 17 (stands)
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                        deck = listOf(Card(Rank.TWO, Suit.SPADES))
                    )
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertTrue(
                state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.PUSH
            )
        }

    @Test
    fun doubleDown_busted_is_DEALER_WON() =
        runTest {
            // Player: NINE + SIX = 15, draws TEN → 25 (bust)
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.NINE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                        deck = listOf(Card(Rank.TEN, Suit.SPADES))
                    )
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertTrue(state.playerHand.isBust)
        }

    @Test
    fun doubleDown_invalid_after_hit() =
        runTest {
            // 3-card hand: cannot double down
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHand =
                        Hand(
                            listOf(
                                Card(Rank.FIVE, Suit.SPADES),
                                Card(Rank.SIX, Suit.HEARTS),
                                Card(Rank.TWO, Suit.CLUBS)
                            )
                        ),
                    dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = listOf(Card(Rank.TWO, Suit.SPADES))
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun doubleDown_invalid_if_insufficient_balance() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 99, // one less than the bet
                    currentBet = 100,
                    playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                    dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = listOf(Card(Rank.TWO, Suit.SPADES))
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun doubleDown_invalid_in_wrong_status() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.BETTING,
                    balance = 900,
                    currentBet = 100,
                    playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                    dealerHand = Hand(listOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = listOf(Card(Rank.TWO, Suit.SPADES))
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    // --- Insurance ---

    @Test
    fun insurance_offered_when_dealer_ace() =
        runTest {
            // Run deals until we observe insurance being offered, verifying dealer up card is Ace
            var sawInsuranceOffered = false
            repeat(300) {
                if (sawInsuranceOffered) return@repeat
                val sm = BlackjackStateMachine(this)
                sm.dispatch(GameAction.PlaceBet(100))
                advanceUntilIdle()
                sm.dispatch(GameAction.Deal)
                advanceUntilIdle()

                val state = sm.state.value
                if (state.status == GameStatus.INSURANCE_OFFERED) {
                    assertEquals(Rank.ACE, state.dealerHand.cards[0].rank)
                    sawInsuranceOffered = true
                }
            }
            assertTrue(sawInsuranceOffered, "Insurance should be offered when dealer shows Ace")
        }

    @Test
    fun insurance_not_offered_when_dealer_non_ace() =
        runTest {
            // When status is PLAYING after deal, dealer up card must not be Ace
            repeat(50) {
                val sm = BlackjackStateMachine(this)
                sm.dispatch(GameAction.PlaceBet(100))
                advanceUntilIdle()
                sm.dispatch(GameAction.Deal)
                advanceUntilIdle()

                val state = sm.state.value
                if (state.status == GameStatus.PLAYING) {
                    assertNotEquals(Rank.ACE, state.dealerHand.cards[0].rank)
                }
            }
        }

    @Test
    fun take_insurance_deducts_half_bet() =
        runTest {
            // INSURANCE_OFFERED, bet=100, balance=900 → TakeInsurance → balance=850, insuranceBet=50, PLAYING
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(850, state.balance)
            assertEquals(50, state.insuranceBet)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun decline_insurance_no_balance_change() =
        runTest {
            // INSURANCE_OFFERED, bet=100, balance=900 → DeclineInsurance → balance=900, insuranceBet=0, PLAYING
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.INSURANCE_OFFERED,
                        balance = 900,
                        currentBet = 100,
                        playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.DeclineInsurance)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(900, state.balance)
            assertEquals(0, state.insuranceBet)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun insurance_pays_on_dealer_blackjack() =
        runTest {
            // Player: TEN + SIX = 16, Dealer: ACE (face-up) + TEN (face-down) = 21 natural BJ
            // Player took insurance: balance=850, insuranceBet=50, currentBet=100
            // Stand → dealer reveals BJ → insurance pays 50*3=150 → balance=1000; DEALER_WON
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
                        currentBet = 100,
                        insuranceBet = 50,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.TEN, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertEquals(1000, state.balance) // 850 + 50*3 = 1000; no regular bet payout (dealer wins)
        }

    @Test
    fun insurance_forfeited_on_no_dealer_blackjack() =
        runTest {
            // Player: TEN + NINE = 19, Dealer: ACE (face-up) + SIX (face-down) = 17 (no BJ, stands)
            // Player took insurance: balance=850, insuranceBet=50, currentBet=100
            // Stand → dealer reveals 17 → no insurance payout; player wins regular bet
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 850,
                        currentBet = 100,
                        insuranceBet = 50,
                        playerHand = Hand(listOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS))),
                        dealerHand =
                            Hand(
                                listOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SIX, Suit.DIAMONDS, isFaceDown = true)
                                )
                            ),
                        deck = emptyList()
                    )
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1050, state.balance) // 850 + 100*2 = 1050; insurance forfeited (already deducted)
        }

    @Test
    fun insurance_invalid_in_wrong_status() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHand = Hand(listOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS))),
                    dealerHand =
                        Hand(
                            listOf(
                                Card(Rank.ACE, Suit.CLUBS),
                                Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true)
                            )
                        ),
                    deck = emptyList()
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }
}
