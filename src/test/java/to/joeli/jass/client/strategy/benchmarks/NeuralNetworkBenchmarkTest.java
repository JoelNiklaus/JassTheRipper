package to.joeli.jass.client.strategy.benchmarks;

import org.junit.Ignore;
import org.junit.Test;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.training.Arena;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static to.joeli.jass.client.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;

@Ignore("On Travis CI this test will not work because of the missing model")
public class NeuralNetworkBenchmarkTest {

	private static final boolean RUN_BENCHMARKS = false;

	private static final long SEED = 42;
	private static final int NUM_GAMES = 10;

	private Arena arena = new Arena(IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	/**
	 * Tests if it is worthwhile to use a score estimator
	 */
	@Test
	public void testScoreEstimatorAgainstRandomPlayout() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, true, false),
					new Config(true, false, false)
			};

			final double performance = arena.runMatchWithConfigs(new Random(SEED), configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}
}
