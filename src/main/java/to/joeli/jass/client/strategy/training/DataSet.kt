package to.joeli.jass.client.strategy.training


import java.util.*

class DataSet(private val cardsFeaturesQueue: Queue<Array<DoubleArray>>,
              private val scoreFeaturesQueue: Queue<Array<DoubleArray>>,
              private val cardsTargetsQueue: Queue<Array<IntArray>>,
              private val scoreTargetsQueue: Queue<Double>) {

    val cardsFeatures: List<Array<DoubleArray>>
        get() = ArrayList(cardsFeaturesQueue)

    val scoreFeatures: List<Array<DoubleArray>>
        get() = ArrayList(scoreFeaturesQueue)

    val cardsTargets: List<Array<IntArray>>
        get() = ArrayList(cardsTargetsQueue)

    val scoreTargets: List<Double>
        get() = ArrayList(scoreTargetsQueue)
}
