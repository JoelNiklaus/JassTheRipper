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

	@BeforeClass
	public static void setUp() {

	}

	@Test
	public void testWinsAgainstChallenge() throws JSONException {
		if (RUN_BENCHMARK) {
			int tournamentRounds = 1;
			int maxPoints = 2500;
			int seed = 42;

			System.out.println("Running benchmark with " + tournamentRounds + "tournament round(s) to " + maxPoints + " points with random seed " + seed + " and with the Challenge bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, tournamentRounds, maxPoints, seed, true);
			assertTrue(BenchmarkRunner.evaluateResult() < 0); // INFO: We want the JassTheRipper bots (Team 2 (!)) to win

			System.out.println("Running benchmark with " + tournamentRounds + "tournament round(s) to " + maxPoints + " points with random seed " + seed + " and with the JassTheRipper bots as team 1");
			BenchmarkRunner.runBenchmark(MY_STRATEGY, BOT_NAME, tournamentRounds, maxPoints, seed, false);
			assertTrue(BenchmarkRunner.evaluateResult() > 0);// INFO: We want the JassTheRipper bots (Team 1 (!)) to win
		}
	}
}
