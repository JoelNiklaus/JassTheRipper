package com.zuehlke.jasschallenge.client.game.strategy.mcts.src;

public enum FinalSelectionPolicy {
	MAX_CHILD, // The child with highest score
	ROBUST_CHILD // The child with highest visit count
}
