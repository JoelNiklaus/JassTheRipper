package to.joeli.jass.client.game.strategy.benchmarks;

import to.joeli.jass.client.game.strategy.JassStrategy;
import to.joeli.jass.client.game.strategy.JassTheRipperJassStrategy;
import org.json.JSONException;
import org.junit.Test;


import static org.junit.Assert.assertTrue;

/**
 * Runs an automated benchmark against the challenge bot. The results are stored in the tournament_logging_dir folder of the jass-server.
 */
public class ChallengeBenchmarkTest {

	private static final boolean RUN_BENCHMARKS = false;

	private final static String BOT_NAME = "JassTheRipper";

	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy();

	private final static int TOURNAMENT_ROUNDS = 1;
	private final static int MAX_POINTS = 2500;
	private final static int SEED = 42;

	@Test
	public void testWinsAgainstChallengeWithChallengeAsTeam1() throws JSONException, InterruptedException {
		Thread.sleep(1000);
		if (RUN_BENCHMARKS) {
			System.out.println("Running benchmark with " + TOURNAMENT_ROUNDS + " tournament round(s) to " + MAX_POINTS + " points with random seed " + SEED + " and with the Challenge bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, TOURNAMENT_ROUNDS, MAX_POINTS, SEED, true);
			assertTrue(BenchmarkRunner.evaluateResult() < 0); // INFO: We want the JassTheRipper bots (Team 2 (!)) to win
		}
	}

	@Test
	public void testWinsAgainstChallengeWithJassTheRipperAsTeam1() throws JSONException, InterruptedException {
		Thread.sleep(1000);
		if (RUN_BENCHMARKS) {
			System.out.println("Running benchmark with " + TOURNAMENT_ROUNDS + " tournament round(s) to " + MAX_POINTS + " points with random seed " + SEED + " and with the JassTheRipper bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, TOURNAMENT_ROUNDS, MAX_POINTS, SEED, false);
			assertTrue(BenchmarkRunner.evaluateResult() > 0); // INFO: We want the JassTheRipper bots (Team 1 (!)) to win
		}
	}
}
