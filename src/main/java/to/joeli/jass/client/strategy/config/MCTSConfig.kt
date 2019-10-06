package to.joeli.jass.client.strategy.config

import to.joeli.jass.client.strategy.mcts.src.FinalSelectionPolicy
import to.joeli.jass.client.strategy.mcts.src.HeuristicFunction
import to.joeli.jass.client.strategy.mcts.src.PlayoutSelectionPolicy
import kotlin.math.sqrt

class MCTSConfig {
    var runMode = RunMode.TIME
    var trumpfStrengthLevel = StrengthLevel.INSANE
    var cardStrengthLevel = StrengthLevel.INSANE

    var cheating = false // enable this for comparing the cards estimator performance to a player who knows all the cards

    var seed = 42
    var scoreBoundsUsed = false
    var explorationConstant = sqrt(2.0)
    var pessimisticBias = 0.0
    var optimisticBias = 0.0
    var numPlayouts = 2 // Scored the best in experiments
    var finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD
    var heuristicFunction: HeuristicFunction? = null
    var playoutSelectionPolicy: PlayoutSelectionPolicy? = null

    constructor() {
        // Different settings on local machine for faster testing
        if (System.getProperty("os.name") == "Mac OS X")
            cardStrengthLevel = StrengthLevel.POWERFUL
    }

    constructor(scoreBoundsUsed: Boolean, pessimisticBias: Double, optimisticBias: Double) : this() {
        this.scoreBoundsUsed = scoreBoundsUsed
        this.pessimisticBias = pessimisticBias
        this.optimisticBias = optimisticBias
    }

    constructor(cheating: Boolean) : this() {
        this.cheating = cheating
    }

    constructor(explorationConstant: Double) : this() {
        this.explorationConstant = explorationConstant
    }

    constructor(cardStrengthLevel: StrengthLevel, numPlayouts: Int) : this() {
        this.cardStrengthLevel = cardStrengthLevel
        this.numPlayouts = numPlayouts
    }

    constructor(cardStrengthLevel: StrengthLevel, runMode: RunMode, numPlayouts: Int) : this() {
        this.cardStrengthLevel = cardStrengthLevel
        this.runMode = runMode
        this.numPlayouts = numPlayouts
    }

    constructor(finalSelectionPolicy: FinalSelectionPolicy) : this() {
        this.finalSelectionPolicy = finalSelectionPolicy
    }

    constructor(trumpfStrengthLevel: StrengthLevel, cardStrengthLevel: StrengthLevel) : this() {
        this.trumpfStrengthLevel = trumpfStrengthLevel
        this.cardStrengthLevel = cardStrengthLevel
    }

    constructor(cardStrengthLevel: StrengthLevel) : this() {
        this.cardStrengthLevel = cardStrengthLevel
    }

    constructor(playoutSelectionPolicy: PlayoutSelectionPolicy?) : this() {
        this.playoutSelectionPolicy = playoutSelectionPolicy
    }

    constructor(cardStrengthLevel: StrengthLevel, playoutSelectionPolicy: PlayoutSelectionPolicy?) : this() {
        this.cardStrengthLevel = cardStrengthLevel
        this.playoutSelectionPolicy = playoutSelectionPolicy
    }
}
