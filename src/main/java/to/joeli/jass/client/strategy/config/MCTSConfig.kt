package to.joeli.jass.client.strategy.config

import to.joeli.jass.client.strategy.mcts.src.FinalSelectionPolicy
import to.joeli.jass.client.strategy.mcts.src.HeuristicFunction
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelection
import kotlin.math.sqrt

class MCTSConfig {
    var runMode = RunMode.TIME
    var trumpfStrengthLevel = StrengthLevel.INSANE
    var cardStrengthLevel = StrengthLevel.POWERFUL

    var cheating = false // enable this for comparing the cards estimator performance to a player who knows all the cards

    var seed = 42
    var scoreBoundsUsed = false
    var explorationConstant = sqrt(2.0)
    var pessimisticBias = 0.0
    var optimisticBias = 0.0
    var numPlayouts = 2 // Scored the best in experiments
    var finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD
    var heuristicFunction: HeuristicFunction? = null
    var playoutPolicy: PlayoutSelection? = null

    constructor()

    constructor(scoreBoundsUsed: Boolean, pessimisticBias: Double, optimisticBias: Double) {
        this.scoreBoundsUsed = scoreBoundsUsed
        this.pessimisticBias = pessimisticBias
        this.optimisticBias = optimisticBias
    }

    constructor(cheating: Boolean) {
        this.cheating = cheating
    }

    constructor(explorationConstant: Double) {
        this.explorationConstant = explorationConstant
    }

    constructor(numPlayouts: Int) {
        this.numPlayouts = numPlayouts
    }

    constructor(finalSelectionPolicy: FinalSelectionPolicy) {
        this.finalSelectionPolicy = finalSelectionPolicy
    }

    constructor(trumpfStrengthLevel: StrengthLevel, cardStrengthLevel: StrengthLevel) {
        this.trumpfStrengthLevel = trumpfStrengthLevel
        this.cardStrengthLevel = cardStrengthLevel
    }
}
