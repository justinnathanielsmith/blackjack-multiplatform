package io.github.smithjustinn.blackjack

import androidx.compose.runtime.Immutable
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
    val actions: Map<Int, StrategyAction>
)

object StrategyProvider {
    private val hardStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("17+", (2..11).associateWith { StrategyAction.STAND }),
            StrategyCell(
                "16",
                (2..6).associateWith { StrategyAction.STAND } + (7..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "15",
                (2..6).associateWith { StrategyAction.STAND } + (7..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "14",
                (2..6).associateWith { StrategyAction.STAND } + (7..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "13",
                (2..6).associateWith { StrategyAction.STAND } + (7..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "12",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.STAND,
                    5 to StrategyAction.STAND,
                    6 to StrategyAction.STAND,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell("11", (2..10).associateWith { StrategyAction.DOUBLE } + mapOf(11 to StrategyAction.HIT)),
            StrategyCell(
                "10",
                (2..9).associateWith { StrategyAction.DOUBLE } + (10..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "9",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.DOUBLE,
                    4 to StrategyAction.DOUBLE,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell("8 or less", (2..11).associateWith { StrategyAction.HIT })
        )

    private val softStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("A,9", (2..11).associateWith { StrategyAction.STAND }),
            StrategyCell("A,8", (2..11).associateWith { StrategyAction.STAND }),
            StrategyCell(
                "A,7",
                mapOf(
                    2 to StrategyAction.STAND,
                    3 to StrategyAction.DOUBLE,
                    4 to StrategyAction.DOUBLE,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.STAND,
                    8 to StrategyAction.STAND,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "A,6",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.DOUBLE,
                    4 to StrategyAction.DOUBLE,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "A,5",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.DOUBLE,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "A,4",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.DOUBLE,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "A,3",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.HIT,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "A,2",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.HIT,
                    5 to StrategyAction.DOUBLE,
                    6 to StrategyAction.DOUBLE,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            )
        )

    private val pairsStrategy: List<StrategyCell> =
        listOf(
            StrategyCell("A,A", (2..11).associateWith { StrategyAction.SPLIT }),
            StrategyCell("10,10", (2..11).associateWith { StrategyAction.STAND }),
            StrategyCell(
                "9,9",
                (2..6).associateWith { StrategyAction.SPLIT } +
                    mapOf(
                        7 to StrategyAction.STAND,
                        8 to StrategyAction.SPLIT,
                        9 to StrategyAction.SPLIT,
                        10 to StrategyAction.STAND,
                        11 to StrategyAction.STAND
                    )
            ),
            StrategyCell("8,8", (2..11).associateWith { StrategyAction.SPLIT }),
            StrategyCell(
                "7,7",
                (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "6,6",
                (2..6).associateWith { StrategyAction.SPLIT } + (7..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "5,5",
                (2..9).associateWith { StrategyAction.DOUBLE } + (10..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "4,4",
                mapOf(
                    2 to StrategyAction.HIT,
                    3 to StrategyAction.HIT,
                    4 to StrategyAction.HIT,
                    5 to StrategyAction.SPLIT,
                    6 to StrategyAction.SPLIT,
                    7 to StrategyAction.HIT,
                    8 to StrategyAction.HIT,
                    9 to StrategyAction.HIT,
                    10 to StrategyAction.HIT,
                    11 to StrategyAction.HIT
                )
            ),
            StrategyCell(
                "3,3",
                (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
            ),
            StrategyCell(
                "2,2",
                (2..7).associateWith { StrategyAction.SPLIT } + (8..11).associateWith { StrategyAction.HIT }
            )
        )

    fun getHardStrategy(): List<StrategyCell> = hardStrategy

    fun getSoftStrategy(): List<StrategyCell> = softStrategy

    fun getPairsStrategy(): List<StrategyCell> = pairsStrategy
}
