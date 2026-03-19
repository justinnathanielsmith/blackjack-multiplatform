package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.Serializable

@Serializable
enum class StrategyAction {
    HIT,
    STAND,
    DOUBLE,
    SPLIT
}

sealed class StrategyTab {
    data object Hard : StrategyTab()

    data object Soft : StrategyTab()

    data object Pairs : StrategyTab()
}

@Immutable
data class StrategyCell(
    val playerValue: String,
    // dealer upcard (2..11) to action
    val actions: ImmutableMap<Int, StrategyAction>
)

@Suppress("MagicNumber")
object StrategyProvider {
    private val ALL_UPCARDS = 2..11

    private val hardStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("17+", ALL_UPCARDS.associateWith { StrategyAction.STAND }.toImmutableMap()),
            StrategyCell(
                "16",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..6
                        ) {
                            StrategyAction.STAND
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "15",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..6
                        ) {
                            StrategyAction.STAND
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "14",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..6
                        ) {
                            StrategyAction.STAND
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "13",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..6
                        ) {
                            StrategyAction.STAND
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "12",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            4..6
                        ) {
                            StrategyAction.STAND
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "11",
                ALL_UPCARDS
                    .associateWith {
                        if (it ==
                            11
                        ) {
                            StrategyAction.HIT
                        } else {
                            StrategyAction.DOUBLE
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "10",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            10..11
                        ) {
                            StrategyAction.HIT
                        } else {
                            StrategyAction.DOUBLE
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "9",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            3..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell("8 or less", ALL_UPCARDS.associateWith { StrategyAction.HIT }.toImmutableMap())
        )

    private val softStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("A,9", ALL_UPCARDS.associateWith { StrategyAction.STAND }.toImmutableMap()),
            StrategyCell(
                "A,8",
                ALL_UPCARDS
                    .associateWith {
                        if (it ==
                            6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.STAND
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,7",
                ALL_UPCARDS
                    .associateWith {
                        when (it) {
                            in 3..6 -> StrategyAction.DOUBLE
                            2, 7, 8 -> StrategyAction.STAND
                            else -> StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,6",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            3..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,5",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            4..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,4",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            4..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,3",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            5..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "A,2",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            5..6
                        ) {
                            StrategyAction.DOUBLE
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            )
        )

    private val pairsStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("A,A", ALL_UPCARDS.associateWith { StrategyAction.SPLIT }.toImmutableMap()),
            StrategyCell("10,10", ALL_UPCARDS.associateWith { StrategyAction.STAND }.toImmutableMap()),
            StrategyCell(
                "9,9",
                ALL_UPCARDS
                    .associateWith {
                        when (it) {
                            7, 10, 11 -> StrategyAction.STAND
                            else -> StrategyAction.SPLIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell("8,8", ALL_UPCARDS.associateWith { StrategyAction.SPLIT }.toImmutableMap()),
            StrategyCell(
                "7,7",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..7
                        ) {
                            StrategyAction.SPLIT
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "6,6",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..6
                        ) {
                            StrategyAction.SPLIT
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "5,5",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            10..11
                        ) {
                            StrategyAction.HIT
                        } else {
                            StrategyAction.DOUBLE
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "4,4",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            5..6
                        ) {
                            StrategyAction.SPLIT
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "3,3",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..7
                        ) {
                            StrategyAction.SPLIT
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            ),
            StrategyCell(
                "2,2",
                ALL_UPCARDS
                    .associateWith {
                        if (it in
                            2..7
                        ) {
                            StrategyAction.SPLIT
                        } else {
                            StrategyAction.HIT
                        }
                    }.toImmutableMap()
            )
        )

    fun getHardStrategy(): List<StrategyCell> = hardStrategy

    fun getSoftStrategy(): List<StrategyCell> = softStrategy

    fun getPairsStrategy(): List<StrategyCell> = pairsStrategy
}
