package com.zuehlke.jasschallenge.client.game.strategy.training;

import org.junit.Test;

import static com.zuehlke.jasschallenge.client.game.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;

public class ArenaTest {

	private Arena arena = new Arena(2, 2, IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	@Test
	public void train() {
		// NOTE: Do not run for normal tests. Takes way too much time.
		// arena.trainForNumEpisodes(1);
	}

	@Test
	public void trainUntilBetterThanRandomPlayouts() {
		// NOTE: Do not run for normal tests. Takes way too much time.
		// arena.trainUntilBetterThanRandomPlayouts();
	}
}
