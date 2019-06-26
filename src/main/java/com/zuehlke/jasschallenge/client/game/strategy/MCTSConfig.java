package com.zuehlke.jasschallenge.client.game.strategy;

import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.FinalSelectionPolicy;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.HeuristicFunction;
import com.zuehlke.jasschallenge.client.game.strategy.mcts.src.PlayoutSelection;

public class MCTSConfig {
	private RunMode runMode = RunMode.TIME;
	private StrengthLevel trumpfStrengthLevel = StrengthLevel.INSANE;
	private StrengthLevel cardStrengthLevel = StrengthLevel.POWERFUL;

	private boolean scoreBounds = false;
	private double explorationConstant = Math.sqrt(2.0);
	private double pessimisticBias = 0.0;
	private double optimisticBias = 0.0;
	private FinalSelectionPolicy finalSelectionPolicy = FinalSelectionPolicy.ROBUST_CHILD;
	private HeuristicFunction heuristicFunction = null;
	private PlayoutSelection playoutPolicy = null;

	public MCTSConfig() {
	}

	public MCTSConfig(StrengthLevel trumpfStrengthLevel, StrengthLevel cardStrengthLevel) {
		this.trumpfStrengthLevel = trumpfStrengthLevel;
		this.cardStrengthLevel = cardStrengthLevel;
	}

	public RunMode getRunMode() {
		return runMode;
	}

	public void setRunMode(RunMode runMode) {
		this.runMode = runMode;
	}

	public StrengthLevel getTrumpfStrengthLevel() {
		return trumpfStrengthLevel;
	}

	public void setTrumpfStrengthLevel(StrengthLevel trumpfStrengthLevel) {
		this.trumpfStrengthLevel = trumpfStrengthLevel;
	}

	public StrengthLevel getCardStrengthLevel() {
		return cardStrengthLevel;
	}

	public void setCardStrengthLevel(StrengthLevel cardStrengthLevel) {
		this.cardStrengthLevel = cardStrengthLevel;
	}

	public boolean isScoreBounds() {
		return scoreBounds;
	}

	public void setScoreBounds(boolean scoreBounds) {
		this.scoreBounds = scoreBounds;
	}

	public double getExplorationConstant() {
		return explorationConstant;
	}

	public void setExplorationConstant(double explorationConstant) {
		this.explorationConstant = explorationConstant;
	}

	public double getPessimisticBias() {
		return pessimisticBias;
	}

	public void setPessimisticBias(double pessimisticBias) {
		this.pessimisticBias = pessimisticBias;
	}

	public double getOptimisticBias() {
		return optimisticBias;
	}

	public void setOptimisticBias(double optimisticBias) {
		this.optimisticBias = optimisticBias;
	}

	public FinalSelectionPolicy getFinalSelectionPolicy() {
		return finalSelectionPolicy;
	}

	public void setFinalSelectionPolicy(FinalSelectionPolicy finalSelectionPolicy) {
		this.finalSelectionPolicy = finalSelectionPolicy;
	}

	public HeuristicFunction getHeuristicFunction() {
		return heuristicFunction;
	}

	public void setHeuristicFunction(HeuristicFunction heuristicFunction) {
		this.heuristicFunction = heuristicFunction;
	}

	public PlayoutSelection getPlayoutPolicy() {
		return playoutPolicy;
	}

	public void setPlayoutPolicy(PlayoutSelection playoutPolicy) {
		this.playoutPolicy = playoutPolicy;
	}
}
