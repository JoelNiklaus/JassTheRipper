package com.zuehlke.jasschallenge.client.game.strategy.mcts.src.main;

public enum FinalSelectionPolicy {
	maxChild, // The child with highest SCORE
	robustChild // The child with highest visit count

}