package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Test;


import static org.junit.Assert.assertTrue;

/**
 * Runs an automated benchmark against the challenge bot. The results are stored in the tournament_logging_dir folder of the jass-server.
 */
public class ChallengeBenchmarkTest {

	private static final boolean RUN_BENCHMARK = false;

	@BeforeClass
	public static void setUp() {
		if (RUN_BENCHMARK) {
			int seed = 42;
			System.out.println("Running Benchmark with random seed " + seed);
			BenchmarkRunner.runBenchmark(seed);
		}
	}

	@Test
	public void testWinsAgainstChallenge() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 0);
	}

	@Test
	public void testWinsAgainstChallengeBy100Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 100);
	}

	@Test
	public void testWinsAgainstChallengeBy200Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 200);
	}

	@Test
	public void testWinsAgainstChallengeBy300Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 300);
	}

	@Test
	public void testWinsAgainstChallengeBy400Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 400);
	}

	@Test
	public void testWinsAgainstChallengeBy500Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 500);
	}

	@Test
	public void testWinsAgainstChallengeBy600Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 600);
	}

	@Test
	public void testWinsAgainstChallengeBy700Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 700);
	}

	@Test
	public void testWinsAgainstChallengeBy800Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 800);
	}

	@Test
	public void testWinsAgainstChallengeBy900Points() throws JSONException {
		int difference = BenchmarkRunner.evaluateResult();
		assertTrue(difference > 900);
	}


}
