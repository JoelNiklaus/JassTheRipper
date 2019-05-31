package com.zuehlke.jasschallenge.client.game.strategy.training;

import org.junit.Test;

public class ArenaTest {

	private final int NUM_EPISODES = 1;
	private final int NUM_TRAINING_GAMES = 2; // Should be an even number
	private final int NUM_TESTING_GAMES = 2; // Should be an even number
	// If the learning network scores more points than the frozen network times this factor, the frozen network gets replaced
	private static final double IMPROVEMENT_THRESHOLD_PERCENTAGE = 105;
	private static final int SEED = 42;

	private Arena arena = new Arena(NUM_TRAINING_GAMES, NUM_TESTING_GAMES, IMPROVEMENT_THRESHOLD_PERCENTAGE, SEED);

	@Test
	public void train() {
		arena.train(NUM_EPISODES);
	}

	@Test
	public void trainUntilBetterThanRandomPlayouts() {
		arena.trainUntilBetterThanRandomPlayouts();
	}
}
