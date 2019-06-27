package com.zuehlke.jasschallenge.client.game.strategy

class Config {

    var mctsConfig = MCTSConfig()

    var isMctsEnabled = true // disable this for pitting only the networks against each other

    var isScoreEstimaterUsed = false // This is used in Self Play Training
    var isCardsEstimaterUsed = false // This is used in Self Play Training
    var isScoreEstimatorTrainable = false // This is used in Self Play Training
    var isCardsEstimatorTrainable = false // This is used in Self Play Training

    // TODO MCTS still does not like to shift by itself. It is forced to shift now because of the rule-based pruning
    //  --> Investigate why MCTS without pruning does not like shifting
    // NOTE: In Situations where shifting is good, MCTS is inferior.
    // In other situations they seem to be comparable
    var trumpfSelectionMethod = TrumpfSelectionMethod.RULE_BASED

    constructor() {}

    constructor(mctsEnabled: Boolean, scoreEstimaterUsed: Boolean, scoreEstimatorTrainable: Boolean) {
        this.isMctsEnabled = mctsEnabled
        this.isScoreEstimaterUsed = scoreEstimaterUsed
        this.isScoreEstimatorTrainable = scoreEstimatorTrainable
    }

    constructor(mctsConfig: MCTSConfig) {
        this.mctsConfig = mctsConfig
    }
}
