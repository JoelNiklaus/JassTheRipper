package to.joeli.jass.client.strategy.config

class Config {

    var mctsConfig = MCTSConfig()

    var isMctsEnabled = true // disable this for pitting only the networks against each other

    var isScoreEstimatorUsed = false // This is used in Self Play Training
    var isCardsEstimatorUsed = false // This is used in Self Play Training
    var isScoreEstimatorTrainable = false // This is used in Self Play Training
    var isCardsEstimatorTrainable = false // This is used in Self Play Training

    // TODO MCTS still does not like to shift by itself. It is forced to shift now because of the rule-based pruning
    //  --> Investigate why MCTS without pruning does not like shifting
    // NOTE: In Situations where shifting is good, MCTS is inferior.
    // In other situations they seem to be comparable but even there MCTS is weaker
    var trumpfSelectionMethod = TrumpfSelectionMethod.RULE_BASED

    constructor()

    constructor(mctsEnabled: Boolean, scoreEstimatorUsed: Boolean, scoreEstimatorTrainable: Boolean) {
        this.isMctsEnabled = mctsEnabled
        this.isScoreEstimatorUsed = scoreEstimatorUsed
        this.isScoreEstimatorTrainable = scoreEstimatorTrainable
    }

    constructor(mctsEnabled: Boolean, cardsEstimatorUsed: Boolean, cardsEstimatorTrainable: Boolean, scoreEstimatorUsed: Boolean, scoreEstimatorTrainable: Boolean) {
        this.isMctsEnabled = mctsEnabled
        this.isCardsEstimatorUsed = cardsEstimatorUsed
        this.isCardsEstimatorTrainable = cardsEstimatorTrainable
        this.isScoreEstimatorUsed = scoreEstimatorUsed
        this.isScoreEstimatorTrainable = scoreEstimatorTrainable
    }

    constructor(mctsConfig: MCTSConfig) {
        this.mctsConfig = mctsConfig
    }
}
