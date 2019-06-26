package com.zuehlke.jasschallenge.client.game.strategy;

public class Config {

	private MCTSConfig mctsConfig = new MCTSConfig();

	private boolean mctsEnabled = true; // disable this for pitting only the networks against each other

	private boolean scoreEstimaterUsed = false; // This is used in Self Play Training
	private boolean cardsEstimaterUsed = false; // This is used in Self Play Training
	private boolean scoreEstimatorTrainable = false; // This is used in Self Play Training
	private boolean cardsEstimatorTrainable = false; // This is used in Self Play Training

	// TODO MCTS still does not like to shift by itself. It is forced to shift now because of the rule-based pruning
	//  --> Investigate why MCTS without pruning does not like shifting
	// NOTE: In Situations where shifting is good, MCTS is inferior.
	// In other situations they seem to be comparable
	private TrumpfSelectionMethod trumpfSelectionMethod = TrumpfSelectionMethod.RULE_BASED;

	public Config() {
	}

	public Config(boolean mctsEnabled, boolean scoreEstimaterUsed, boolean scoreEstimatorTrainable) {
		this.mctsEnabled = mctsEnabled;
		this.scoreEstimaterUsed = scoreEstimaterUsed;
		this.scoreEstimatorTrainable = scoreEstimatorTrainable;
	}

	public Config(MCTSConfig mctsConfig) {
		this.mctsConfig = mctsConfig;
	}

	public MCTSConfig getMctsConfig() {
		return mctsConfig;
	}

	public void setMctsConfig(MCTSConfig mctsConfig) {
		this.mctsConfig = mctsConfig;
	}

	public boolean isMctsEnabled() {
		return mctsEnabled;
	}

	public void setMctsEnabled(boolean mctsEnabled) {
		this.mctsEnabled = mctsEnabled;
	}

	public boolean isScoreEstimaterUsed() {
		return scoreEstimaterUsed;
	}

	public void setScoreEstimaterUsed(boolean scoreEstimaterUsed) {
		this.scoreEstimaterUsed = scoreEstimaterUsed;
	}

	public boolean isCardsEstimaterUsed() {
		return cardsEstimaterUsed;
	}

	public void setCardsEstimaterUsed(boolean cardsEstimaterUsed) {
		this.cardsEstimaterUsed = cardsEstimaterUsed;
	}

	public boolean isScoreEstimatorTrainable() {
		return scoreEstimatorTrainable;
	}

	public void setScoreEstimatorTrainable(boolean scoreEstimatorTrainable) {
		this.scoreEstimatorTrainable = scoreEstimatorTrainable;
	}

	public boolean isCardsEstimatorTrainable() {
		return cardsEstimatorTrainable;
	}

	public void setCardsEstimatorTrainable(boolean cardsEstimatorTrainable) {
		this.cardsEstimatorTrainable = cardsEstimatorTrainable;
	}

	public TrumpfSelectionMethod getTrumpfSelectionMethod() {
		return trumpfSelectionMethod;
	}

	public void setTrumpfSelectionMethod(TrumpfSelectionMethod trumpfSelectionMethod) {
		this.trumpfSelectionMethod = trumpfSelectionMethod;
	}
}
