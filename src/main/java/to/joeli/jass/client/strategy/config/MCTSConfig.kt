package to.joeli.jass.client.strategy.config

import to.joeli.jass.client.strategy.mcts.src.FinalSelectionPolicy
import to.joeli.jass.client.strategy.mcts.src.HeuristicFunction
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelection
import kotlin.math.sqrt

class MCTSConfig {
    var runMode = RunMode.TIME
    var trumpfStrengthLevel = StrengthLevel.INSANE
    var cardStrengthLevel = StrengthLevel.POWERFUL

    var seed = 42
    var scoreBoundsUsed = false
    var explorationConstant = sqrt(2.0)
    var pessimisticBias = 0.0
    var optimisticBias = 0.0
    var finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD
    var heuristicFunction: HeuristicFunction? = null
    var playoutPolicy: PlayoutSelection? = null

    constructor()

    constructor(trumpfStrengthLevel: StrengthLevel, cardStrengthLevel: StrengthLevel) {
        this.trumpfStrengthLevel = trumpfStrengthLevel
        this.cardStrengthLevel = cardStrengthLevel
    }
}