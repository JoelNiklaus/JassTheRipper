package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import com.zuehlke.jasschallenge.client.game.strategy.Config;
import com.zuehlke.jasschallenge.client.game.strategy.training.Arena;
import org.junit.Test;

import java.util.Random;

import static com.zuehlke.jasschallenge.client.game.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;
import static org.junit.Assert.assertTrue;

public class NeuralNetworkBenchmarkTest {

	private static final boolean RUN_BENCHMARKS = false;

	private static final long SEED = 42;
	private static final int NUM_GAMES = 10;

	private Arena arena = new Arena(Arena.SCORE_ESTIMATOR_PATH, 2, 2, IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	/**
	 * Tests if it is worthwhile to use a score estimator
	 */
	@Test
	public void testScoreEstimatorAgainstRandomPlayout() {
		// NOTE: Because the MCTS Trumpf Selection almost never shifts, it is inferior to the rule-based one!
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, true, false),
					new Config(true, false, false)
			};

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}
}
