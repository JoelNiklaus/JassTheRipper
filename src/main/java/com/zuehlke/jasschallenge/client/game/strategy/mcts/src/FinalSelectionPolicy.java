package com.zuehlke.jasschallenge.client.game.strategy.mcts;

public enum FinalSelectionPolicy {
	maxChild, // The child with highest SCORE
	robustChild // The child with highest visit count

}
