package io.github.smithjustinn.blackjack.ui.components.cards

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import io.github.smithjustinn.blackjack.model.Rank
import io.github.smithjustinn.blackjack.model.Suit
import io.github.smithjustinn.blackjack.ui.theme.PokerBlack
import io.github.smithjustinn.blackjack.ui.theme.PokerRed
import org.jetbrains.compose.resources.StringResource
import sharedui.generated.resources.Res
import sharedui.generated.resources.rank_ace
import sharedui.generated.resources.rank_eight
import sharedui.generated.resources.rank_five
import sharedui.generated.resources.rank_four
import sharedui.generated.resources.rank_jack
import sharedui.generated.resources.rank_king
import sharedui.generated.resources.rank_name_ace
import sharedui.generated.resources.rank_name_eight
import sharedui.generated.resources.rank_name_five
import sharedui.generated.resources.rank_name_four
import sharedui.generated.resources.rank_name_jack
import sharedui.generated.resources.rank_name_king
import sharedui.generated.resources.rank_name_nine
import sharedui.generated.resources.rank_name_queen
import sharedui.generated.resources.rank_name_seven
import sharedui.generated.resources.rank_name_six
import sharedui.generated.resources.rank_name_ten
import sharedui.generated.resources.rank_name_three
import sharedui.generated.resources.rank_name_two
import sharedui.generated.resources.rank_nine
import sharedui.generated.resources.rank_queen
import sharedui.generated.resources.rank_seven
import sharedui.generated.resources.rank_six
import sharedui.generated.resources.rank_ten
import sharedui.generated.resources.rank_three
import sharedui.generated.resources.rank_two
import sharedui.generated.resources.suit_clubs
import sharedui.generated.resources.suit_diamonds
import sharedui.generated.resources.suit_hearts
import sharedui.generated.resources.suit_name_clubs
import sharedui.generated.resources.suit_name_diamonds
import sharedui.generated.resources.suit_name_hearts
import sharedui.generated.resources.suit_name_spades
import sharedui.generated.resources.suit_spades

internal fun shadowStyle(
    color: Color,
    offset: Offset,
    blur: Float
) = TextStyle(shadow = Shadow(color = color, offset = offset, blurRadius = blur))

val Suit.color: Color
    get() =
        when (this) {
            Suit.HEARTS, Suit.DIAMONDS -> PokerRed
            Suit.CLUBS, Suit.SPADES -> PokerBlack
        }

val Suit.symbolRes: StringResource
    get() =
        when (this) {
            Suit.HEARTS -> Res.string.suit_hearts
            Suit.DIAMONDS -> Res.string.suit_diamonds
            Suit.CLUBS -> Res.string.suit_clubs
            Suit.SPADES -> Res.string.suit_spades
        }

val Suit.nameRes: StringResource
    get() =
        when (this) {
            Suit.HEARTS -> Res.string.suit_name_hearts
            Suit.DIAMONDS -> Res.string.suit_name_diamonds
            Suit.CLUBS -> Res.string.suit_name_clubs
            Suit.SPADES -> Res.string.suit_name_spades
        }

val Rank.symbolRes: StringResource
    get() =
        when (this) {
            Rank.TWO -> Res.string.rank_two
            Rank.THREE -> Res.string.rank_three
            Rank.FOUR -> Res.string.rank_four
            Rank.FIVE -> Res.string.rank_five
            Rank.SIX -> Res.string.rank_six
            Rank.SEVEN -> Res.string.rank_seven
            Rank.EIGHT -> Res.string.rank_eight
            Rank.NINE -> Res.string.rank_nine
            Rank.TEN -> Res.string.rank_ten
            Rank.JACK -> Res.string.rank_jack
            Rank.QUEEN -> Res.string.rank_queen
            Rank.KING -> Res.string.rank_king
            Rank.ACE -> Res.string.rank_ace
        }

val Rank.nameRes: StringResource
    get() =
        when (this) {
            Rank.TWO -> Res.string.rank_name_two
            Rank.THREE -> Res.string.rank_name_three
            Rank.FOUR -> Res.string.rank_name_four
            Rank.FIVE -> Res.string.rank_name_five
            Rank.SIX -> Res.string.rank_name_six
            Rank.SEVEN -> Res.string.rank_name_seven
            Rank.EIGHT -> Res.string.rank_name_eight
            Rank.NINE -> Res.string.rank_name_nine
            Rank.TEN -> Res.string.rank_name_ten
            Rank.JACK -> Res.string.rank_name_jack
            Rank.QUEEN -> Res.string.rank_name_queen
            Rank.KING -> Res.string.rank_name_king
            Rank.ACE -> Res.string.rank_name_ace
        }
