package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import com.zuehlke.jasschallenge.client.game.strategy.JassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertTrue;

/**
 * Runs an automated benchmark against the challenge bot. The results are stored in the tournament_logging_dir folder of the jass-server.
 */
public class ChallengeBenchmarkTest {

	private static final boolean RUN_BENCHMARK = true;

	private final static String BOT_NAME = "JassTheRipper";

	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy(StrengthLevel.STRONG);

	private final static int TOURNAMENT_ROUNDS = 1;
	private final static int MAX_POINTS = 2500;
	private final static int SEED = 42;

	@BeforeClass
	public static void setUp() {

	}

	@Test
	public void testWinsAgainstChallengeWithChallengeAsTeam1() throws JSONException {
		if (RUN_BENCHMARK) {
			System.out.println("Running benchmark with " + TOURNAMENT_ROUNDS + " tournament round(s) to " + MAX_POINTS + " points with random seed " + SEED + " and with the Challenge bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, TOURNAMENT_ROUNDS, MAX_POINTS, SEED, true);
			assertTrue(BenchmarkRunner.evaluateResult() < 0); // INFO: We want the JassTheRipper bots (Team 2 (!)) to win
		}
	}

	@Test
	public void testWinsAgainstChallengeWithJassTheRipperAsTeam1() throws JSONException {
		if (RUN_BENCHMARK) {
			System.out.println("Running benchmark with " + TOURNAMENT_ROUNDS + " tournament round(s) to " + MAX_POINTS + " points with random seed " + SEED + " and with the JassTheRipper bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, TOURNAMENT_ROUNDS, MAX_POINTS, SEED, false);
			assertTrue(BenchmarkRunner.evaluateResult() > 0); // INFO: We want the JassTheRipper bots (Team 1 (!)) to win
		}
	}
}
