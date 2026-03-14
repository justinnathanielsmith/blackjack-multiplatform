package io.github.smithjustinn.blackjack

import kotlinx.collections.immutable.persistentListOf
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
            assertEquals(0, state.playerHands[0].cards.size)
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
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(2, state.dealerHand.cards.size)
            assertEquals(308, state.deck.size)
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

            val initialPlayerCards =
                stateMachine.state.value.playerHands[0]
                    .cards.size
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(
                initialPlayerCards + 1,
                stateMachine.state.value.playerHands[0]
                    .cards.size
            )
            if (stateMachine.state.value.status == GameStatus.PLAYING) {
                assertEquals(307, stateMachine.state.value.deck.size)
            }
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

            if (stateMachine.state.value.playerHands[0]
                    .isBust
            ) {
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.KING, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.NINE, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.NINE, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(Card(Rank.FIVE, Suit.SPADES)), // Player draws 5 → 25 bust
                    ),
                )
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            // Dealer hole card is revealed after player busts
            assertFalse(state.dealerHand.cards[1].isFaceDown)
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
                    ),
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(200, state.playerBets[0])
            assertEquals(800, state.balance)
            assertEquals(3, state.playerHands[0].cards.size)
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
                    ),
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertTrue(
                state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.PUSH,
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.NINE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(Card(Rank.TEN, Suit.SPADES)),
                    ),
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.DEALER_WON, state.status)
            assertTrue(state.playerHands[0].isBust)
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
                    playerHands =
                        persistentListOf(
                            Hand(
                                persistentListOf(
                                    Card(Rank.FIVE, Suit.SPADES),
                                    Card(Rank.SIX, Suit.HEARTS),
                                    Card(Rank.TWO, Suit.CLUBS),
                                ),
                            ),
                        ),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
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
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
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
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.TEN, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(),
                    ),
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
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SIX, Suit.DIAMONDS, isFaceDown = true),
                                ),
                            ),
                        deck = persistentListOf(),
                    ),
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
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand =
                        Hand(
                            persistentListOf(
                                Card(Rank.ACE, Suit.CLUBS),
                                Card(Rank.SEVEN, Suit.DIAMONDS, isFaceDown = true),
                            ),
                        ),
                    deck = persistentListOf(),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.TakeInsurance)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    // --- Splitting ---

    @Test
    fun split_creates_two_hands() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.EIGHT, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck =
                        persistentListOf(
                            Card(Rank.TWO, Suit.SPADES),
                            Card(Rank.THREE, Suit.HEARTS),
                            Card(Rank.FOUR, Suit.CLUBS),
                        ),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHands[0].cards.size)
            assertEquals(
                2,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.size
            )
            assertEquals(Rank.EIGHT, state.playerHands[0].cards[0].rank)
            assertEquals(
                Rank.EIGHT,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.get(0)
                    ?.rank
            )
            assertEquals(1, state.deck.size) // started with 3, used 2
        }

    @Test
    fun split_deducts_balance() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 900,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.EIGHT, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.HEARTS)),
                    ),
                )
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(800, state.balance)
            assertEquals(100, state.playerBets.getOrNull(1))
        }

    @Test
    fun split_invalid_non_pair() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.HEARTS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun split_invalid_insufficient_balance() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 50,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.EIGHT, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.HEARTS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun split_invalid_after_hit() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(
                                persistentListOf(
                                    Card(Rank.EIGHT, Suit.SPADES),
                                    Card(Rank.EIGHT, Suit.HEARTS),
                                    Card(Rank.TWO, Suit.CLUBS),
                                ),
                            ),
                        ),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES), Card(Rank.THREE, Suit.HEARTS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun split_invalid_when_already_split() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.FOUR, Suit.SPADES), Card(Rank.FIVE, Suit.HEARTS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun hit_routes_to_active_hand() =
        runTest {
            // activeHandIndex=0 → hit goes to playerHands[0], playerHands[1] unchanged
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.FOUR, Suit.SPADES)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(3, state.playerHands[0].cards.size) // primary got card
            assertEquals(
                2,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.size
            ) // split unchanged
        }

    @Test
    fun hit_routes_to_split_hand() =
        runTest {
            // activeHandIndex=1 → hit goes to playerHands[1], playerHands[0] unchanged
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.FOUR, Suit.SPADES)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHands[0].cards.size) // primary unchanged
            assertEquals(
                3,
                state.playerHands
                    .getOrNull(1)
                    ?.cards
                    ?.size
            ) // split got card
        }

    @Test
    fun stand_advances_to_split_hand() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun stand_enters_dealer_turn_after_split_hand() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                            Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun split_independent_payouts_primary_wins_split_loses() =
        runTest {
            // Primary: TEN + TEN = 20 (wins vs dealer 18), Split: EIGHT + EIGHT + SIX = 22 (bust)
            // balance=800, currentBet=100, playerBets=[100,100]
            // Primary payout: 200, Split payout: 0 → balance = 800 + 200 = 1000
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                            Hand(
                                persistentListOf(
                                    Card(Rank.EIGHT, Suit.CLUBS),
                                    Card(Rank.EIGHT, Suit.DIAMONDS),
                                    Card(Rank.SIX, Suit.SPADES),
                                ),
                            ),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.EIGHT, Suit.HEARTS))),
                    deck = persistentListOf(),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1000, state.balance)
        }

    @Test
    fun split_independent_payouts_both_win() =
        runTest {
            // Primary: TEN + TEN = 20, Split: TEN + NINE = 19, Dealer: TEN + SIX draws KING → 26 (bust)
            // balance=800, currentBet=100, playerBets=[100,100]
            // Primary payout: 200, Split payout: 200 → balance = 800 + 400 = 1200
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                            Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 1,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.HEARTS), Card(Rank.SIX, Suit.SPADES))),
                    deck = persistentListOf(Card(Rank.KING, Suit.CLUBS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1200, state.balance)
        }

    @Test
    fun split_ace_no_extra_hit() =
        runTest {
            // Split aces: hit should be blocked
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.FIVE, Suit.HEARTS))),
                            Hand(persistentListOf(Card(Rank.ACE, Suit.CLUBS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun split_ace_auto_stands_after_deal() =
        runTest {
            // Split aces → dealer turn should auto-run, no player input needed
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.ACE, Suit.SPADES), Card(Rank.ACE, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.THREE, Suit.CLUBS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun bust_on_primary_hand_advances_to_split() =
        runTest {
            // Primary: TEN + FIVE = 15, hit TEN → 25 (bust) → advances to split hand, game continues
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 800,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.FIVE, Suit.HEARTS))),
                            Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.THREE, Suit.DIAMONDS))),
                        ),
                    playerBets = persistentListOf(100, 100),
                    activeHandIndex = 0,
                    dealerHand = Hand(persistentListOf(Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))),
                    deck = persistentListOf(Card(Rank.TEN, Suit.HEARTS)),
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
            assertTrue(state.playerHands[0].isBust)
        }

    // --- Multi-Hand ---

    @Test
    fun selectHandCount_updates_handCount() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            assertEquals(3, stateMachine.state.value.handCount)
        }

    @Test
    fun selectHandCount_ignored_outside_betting() =
        runTest {
            val initialState =
                GameState(
                    status = GameStatus.PLAYING,
                    balance = 900,
                    currentBet = 100,
                    playerHands =
                        persistentListOf(
                            Hand(persistentListOf(Card(Rank.FIVE, Suit.SPADES), Card(Rank.SIX, Suit.HEARTS)))
                        ),
                    playerBets = persistentListOf(100),
                    handCount = 1,
                )
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun selectHandCount_ignores_invalid_values() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(this, GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0))
            stateMachine.dispatch(GameAction.SelectHandCount(0))
            advanceUntilIdle()
            assertEquals(1, stateMachine.state.value.handCount)

            stateMachine.dispatch(GameAction.SelectHandCount(4))
            advanceUntilIdle()
            assertEquals(1, stateMachine.state.value.handCount)
        }

    @Test
    fun deal_creates_two_hands_when_handCount_2() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 100, handCount = 2),
                )
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(2, state.playerHands.size)
            assertEquals(persistentListOf(100, 100), state.playerBets)
        }

    @Test
    fun deal_does_not_deduct_balance_for_multi_hand() =
        runTest {
            // 3 hands, bet=100: all 3 hands already paid via PlaceBet → balance=700
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3),
                )
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            // balance should remain 700 — no additional deduction at deal
            assertEquals(700, state.balance)
        }

    @Test
    fun placeBet_rejected_when_total_cost_exceeds_balance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 25, currentBet = 0, handCount = 3)
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.PlaceBet(10)) // total cost = 30 > 25
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun placeBet_deducts_total_cost_for_all_hands() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 3),
                )
            stateMachine.dispatch(GameAction.PlaceBet(10))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(970, state.balance)
            assertEquals(10, state.currentBet)
        }

    @Test
    fun resetBet_refunds_total_cost() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3),
                )
            stateMachine.dispatch(GameAction.ResetBet)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(0, state.currentBet)
        }

    @Test
    fun selectHandCount_adjusts_balance_when_bet_placed() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 900, currentBet = 100, handCount = 1),
                )
            stateMachine.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(700, state.balance)
            assertEquals(3, state.handCount)
        }

    @Test
    fun selectHandCount_refunds_balance_when_reducing_hands() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 700, currentBet = 100, handCount = 3),
                )
            stateMachine.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(800, state.balance)
            assertEquals(2, state.handCount)
        }

    @Test
    fun selectHandCount_rejected_if_insufficient_balance() =
        runTest {
            val initialState = GameState(status = GameStatus.BETTING, balance = 50, currentBet = 100, handCount = 1)
            val stateMachine = BlackjackStateMachine(this, initialState)
            stateMachine.dispatch(GameAction.SelectHandCount(2))
            advanceUntilIdle()

            assertEquals(initialState, stateMachine.state.value)
        }

    @Test
    fun selectHandCount_no_balance_change_when_no_bet() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.BETTING, balance = 1000, currentBet = 0, handCount = 1),
                )
            stateMachine.dispatch(GameAction.SelectHandCount(3))
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance)
            assertEquals(3, state.handCount)
        }

    @Test
    fun stand_advances_to_next_hand() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 0,
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun stand_enters_dealer_turn_on_last_hand() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.TWO, Suit.CLUBS))),
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertTrue(
                state.status == GameStatus.PLAYER_WON ||
                    state.status == GameStatus.DEALER_WON ||
                    state.status == GameStatus.PUSH,
            )
        }

    @Test
    fun hit_bust_advances_to_next_hand() =
        runTest {
            // Hand0: TEN+FIVE=15, draws TEN → bust; hand1 becomes active
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.FIVE, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.THREE, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 0,
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(Card(Rank.TEN, Suit.HEARTS)),
                    ),
                )
            stateMachine.dispatch(GameAction.Hit)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1, state.activeHandIndex)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun multi_hand_independent_payouts() =
        runTest {
            // Hand0: TEN+TEN=20 wins vs dealer 18; Hand1: TEN+TEN+SIX=26 bust
            // Expected: balance += 200 (hand0 win payout); hand1 payout = 0
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                                Hand(
                                    persistentListOf(
                                        Card(Rank.TEN, Suit.CLUBS),
                                        Card(Rank.TEN, Suit.DIAMONDS),
                                        Card(Rank.SIX, Suit.SPADES),
                                    ),
                                ),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.EIGHT, Suit.HEARTS))),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1000, state.balance) // 800 + 200
        }

    @Test
    fun multi_hand_player_won_if_any_hand_wins() =
        runTest {
            // Hand0 wins, Hand1 busts
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                                Hand(
                                    persistentListOf(
                                        Card(Rank.TEN, Suit.CLUBS),
                                        Card(Rank.TEN, Suit.DIAMONDS),
                                        Card(Rank.SIX, Suit.SPADES),
                                    ),
                                ),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.EIGHT, Suit.HEARTS))),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(GameStatus.PLAYER_WON, stateMachine.state.value.status)
        }

    @Test
    fun multi_hand_dealer_won_if_all_hands_lose() =
        runTest {
            // Both hands lose to dealer 20
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.FIVE, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.NINE, Suit.CLUBS), Card(Rank.FOUR, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.TEN, Suit.HEARTS))),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            assertEquals(GameStatus.DEALER_WON, stateMachine.state.value.status)
        }

    @Test
    fun multi_hand_push_if_all_hands_push() =
        runTest {
            // Both hands push dealer at 19
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.NINE, Suit.CLUBS), Card(Rank.TEN, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        dealerHand = Hand(persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.NINE, Suit.SPADES))),
                        deck = persistentListOf(),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PUSH, state.status)
            assertEquals(1000, state.balance) // 800 + 100 + 100 (both bets returned)
        }

    @Test
    fun split_inserts_hand_at_active_index_plus_one() =
        runTest {
            // 2 initial hands; split hand0 (pair of 8s) → 3 hands, new hand at index 1
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.SPADES), Card(Rank.EIGHT, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.SEVEN, Suit.CLUBS), Card(Rank.THREE, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 0,
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck =
                            persistentListOf(
                                Card(Rank.TWO, Suit.SPADES),
                                Card(Rank.THREE, Suit.HEARTS),
                                Card(Rank.FOUR, Suit.CLUBS)
                            ),
                    ),
                )
            stateMachine.dispatch(GameAction.Split)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(3, state.playerHands.size)
            assertEquals(Rank.EIGHT, state.playerHands[1].cards[0].rank) // new split hand at index 1
        }

    @Test
    fun double_down_updates_active_bet_only() =
        runTest {
            // 3 hands; activeHandIndex=1; doubling should only affect playerBets[1]
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.NINE, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS))),
                                Hand(persistentListOf(Card(Rank.EIGHT, Suit.HEARTS), Card(Rank.SEVEN, Suit.SPADES))),
                            ),
                        playerBets = persistentListOf(100, 100, 100),
                        activeHandIndex = 1,
                        dealerHand =
                            Hand(
                                persistentListOf(Card(Rank.TEN, Suit.CLUBS), Card(Rank.SEVEN, Suit.DIAMONDS))
                            ),
                        deck = persistentListOf(Card(Rank.TWO, Suit.SPADES)),
                    ),
                )
            stateMachine.dispatch(GameAction.DoubleDown)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(100, state.playerBets[0]) // unchanged
            assertEquals(200, state.playerBets[1]) // doubled
            assertEquals(100, state.playerBets[2]) // unchanged
        }

    @Test
    fun new_game_resets_hand_count_to_1() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(status = GameStatus.DEALER_WON, balance = 800, currentBet = 0, handCount = 3),
                )
            stateMachine.dispatch(GameAction.NewGame())
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(1, state.handCount)
            assertEquals(1, state.playerHands.size)
        }

    @Test
    fun newGame_uses_rules_deckCount() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.NewGame(rules = GameRules(deckCount = 2)))
            advanceUntilIdle()

            // After deal, deck size should be 104 - 2 (player) - 2 (dealer) = 100
            stateMachine.dispatch(GameAction.PlaceBet(100))
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(100, state.deck.size)
        }

    @Test
    fun newGame_default_deckCount_is_6() =
        runTest {
            val stateMachine = BlackjackStateMachine(this)
            stateMachine.dispatch(GameAction.NewGame())
            advanceUntilIdle()

            // After deal, deck size should be (6 * 52) - 2 (player) - 2 (dealer) = 312 - 4 = 308
            stateMachine.dispatch(GameAction.PlaceBet(100))
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(308, state.deck.size)
        }

    @Test
    fun surrender_valid_on_first_decision() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SEVEN, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS).copy(isFaceDown = true)
                                )
                            ),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            stateMachine.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(850, state.balance) // 800 + 100/2
            assertEquals(GameStatus.DEALER_WON, state.status)
        }

    @Test
    fun surrender_invalid_after_hit() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(
                                    persistentListOf(
                                        Card(Rank.TEN, Suit.SPADES),
                                        Card(Rank.TWO, Suit.HEARTS),
                                        Card(Rank.FIVE, Suit.CLUBS)
                                    )
                                )
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS).copy(isFaceDown = true)
                                )
                            ),
                        rules = GameRules(allowSurrender = true),
                    ),
                )
            stateMachine.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(800, state.balance)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun surrender_no_op_when_rule_disabled() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 800,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.SEVEN, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.TEN, Suit.CLUBS),
                                    Card(Rank.SEVEN, Suit.DIAMONDS).copy(isFaceDown = true)
                                )
                            ),
                        rules = GameRules(allowSurrender = false),
                    ),
                )
            stateMachine.dispatch(GameAction.Surrender)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(800, state.balance)
            assertEquals(GameStatus.PLAYING, state.status)
        }

    @Test
    fun blackjack_payout_3_to_2() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        currentBet = 100,
                        rules = GameRules(blackjackPayout = BlackjackPayout.THREE_TO_TWO),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES), // Hand0 - card1
                                Card(Rank.TEN, Suit.HEARTS), // Hand0 - card2 (BJ)
                                Card(Rank.TEN, Suit.CLUBS), // Dealer - card1
                                Card(Rank.EIGHT, Suit.DIAMONDS), // Dealer - card2
                            ),
                        handCount = 1,
                    ),
                )
            // handleDeal in code:
            // val hands = List(current.handCount) { i -> Hand(persistentListOf(fullDeck[i], fullDeck[i + current.handCount])) }.toPersistentList()
            // if handCount=1: hands[0] = Hand(fullDeck[0], fullDeck[1]) = ACE, TEN. CORRECT.
            // val dealerCards = fullDeck.drop(deckOffset).take(2) where deckOffset = handCount * 2 = 2
            // dealerCards = fullDeck.drop(2).take(2) = TEN, EIGHT. CORRECT.
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1250, state.balance)
        }

    @Test
    fun blackjack_payout_6_to_5() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.BETTING,
                        balance = 1000,
                        currentBet = 100,
                        rules = GameRules(blackjackPayout = BlackjackPayout.SIX_TO_FIVE),
                        deck =
                            persistentListOf(
                                Card(Rank.ACE, Suit.SPADES),
                                Card(Rank.TEN, Suit.HEARTS),
                                Card(Rank.TEN, Suit.CLUBS),
                                Card(Rank.EIGHT, Suit.DIAMONDS),
                            ),
                        handCount = 1,
                    ),
                )
            stateMachine.dispatch(GameAction.Deal)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(GameStatus.PLAYER_WON, state.status)
            assertEquals(1220, state.balance)
        }

    @Test
    fun dealer_hits_soft17_enabled() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 1000,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand =
                            Hand(
                                persistentListOf(
                                    Card(Rank.ACE, Suit.CLUBS),
                                    Card(Rank.SIX, Suit.DIAMONDS).copy(isFaceDown = false)
                                )
                            ),
                        deck = persistentListOf(Card(Rank.TEN, Suit.CLUBS)),
                        rules = GameRules(dealerHitsSoft17 = true),
                    ),
                )
            // Ensure status is correctly set to PLAYING before Stand
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            assertEquals(3, state.dealerHand.cards.size)
        }

    @Test
    fun dealer_stands_soft17_disabled() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 1000,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS)))
                            ),
                        playerBets = persistentListOf(100),
                        dealerHand = Hand(persistentListOf(Card(Rank.ACE, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS))),
                        deck = persistentListOf(Card(Rank.TEN, Suit.CLUBS)),
                        rules = GameRules(dealerHitsSoft17 = false),
                    ),
                )
            stateMachine.dispatch(GameAction.Stand)
            advanceUntilIdle()

            val state = stateMachine.state.value
            // Dealer had Soft 17, should stand.
            assertEquals(2, state.dealerHand.cards.size)
        }

    @Test
    fun double_after_split_allowed() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 1000,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        rules = GameRules(allowDoubleAfterSplit = true),
                    ),
                )
            assertTrue(stateMachine.state.value.canDoubleDown())
        }

    @Test
    fun double_after_split_disallowed() =
        runTest {
            val stateMachine =
                BlackjackStateMachine(
                    this,
                    GameState(
                        status = GameStatus.PLAYING,
                        balance = 1000,
                        currentBet = 100,
                        playerHands =
                            persistentListOf(
                                Hand(persistentListOf(Card(Rank.TEN, Suit.SPADES), Card(Rank.TEN, Suit.HEARTS))),
                                Hand(persistentListOf(Card(Rank.FIVE, Suit.CLUBS), Card(Rank.SIX, Suit.DIAMONDS))),
                            ),
                        playerBets = persistentListOf(100, 100),
                        activeHandIndex = 1,
                        rules = GameRules(allowDoubleAfterSplit = false),
                    ),
                )
            assertFalse(stateMachine.state.value.canDoubleDown())
        }
}
