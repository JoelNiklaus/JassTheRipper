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

	private final static JassStrategy MY_STRATEGY = new JassTheRipperJassStrategy(StrengthLevel.FAST_TEST);

	@BeforeClass
	public static void setUp() {
		if (RUN_BENCHMARK) {
			int tournamentRounds = 1;
			int maxPoints = 2500;
			int seed = 42;
			System.out.println("Running benchmark with " + tournamentRounds + "tournament round(s) to " + maxPoints + " points with random seed " + seed);
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, tournamentRounds, maxPoints, seed);
		}
	}

	@Test
	public void testWinsAgainstChallenge() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 0);
	}

	@Test
	public void testWinsAgainstChallengeByMargin() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 100);
		assertTrue(difference > 200);
		assertTrue(difference > 300);
		assertTrue(difference > 400);
		assertTrue(difference > 500);
	}
}
