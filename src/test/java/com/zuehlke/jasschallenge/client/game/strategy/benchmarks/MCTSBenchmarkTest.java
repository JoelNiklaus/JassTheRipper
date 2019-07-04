package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import com.zuehlke.jasschallenge.client.game.strategy.config.Config;
import com.zuehlke.jasschallenge.client.game.strategy.config.MCTSConfig;
import com.zuehlke.jasschallenge.client.game.strategy.config.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.config.TrumpfSelectionMethod;
import com.zuehlke.jasschallenge.client.game.strategy.training.Arena;
import org.junit.Test;

import java.util.Random;

import static com.zuehlke.jasschallenge.client.game.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;
import static org.junit.Assert.assertTrue;

public class MCTSBenchmarkTest {

	private static final boolean RUN_BENCHMARKS = false;

	private static final long SEED = 42;
	private static final int NUM_GAMES = 10;

	private Arena arena = new Arena(2, 2, IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	/**
	 * Tests if it is worthwhile to use the MCTS trumpf selection method
	 */
	@Test
	public void testRuleBasedTrumpfAgainstMCTSTrumpf() {
		// NOTE: Because the MCTS Trumpf Selection almost never shifts, it is inferior to the rule-based one!
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST));
			configs[1].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST));
			configs[0].setTrumpfSelectionMethod(TrumpfSelectionMethod.RULE_BASED);
			configs[1].setTrumpfSelectionMethod(TrumpfSelectionMethod.MCTS);

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}

	/**
	 * Tests if it is worthwhile to use more search time
	 */
	@Test
	public void testHigherCardStrengthLevelTimeIsWorthwile() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST_STRONG_TIME));
			configs[1].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST_WEAK_TIME));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}


	/**
	 * Tests if it is worthwhile to use more determinizations
	 */
	@Test
	public void testHigherCardStrengthLevelNumDeterminizationsIsWorthwile() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST_STRONG_NUM_DETERMINIZATIONS));
			configs[1].setMctsConfig(new MCTSConfig(StrengthLevel.TRUMPF, StrengthLevel.TEST_WEAK_NUM_DETERMINIZATIONS));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}


	@Test
	public void testHigherTrumpfStrengthLevelIsWorthwile() {
		// NOTE: This makes only sense when the MCTS TrumpfSelectionMethod is used.
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(StrengthLevel.STRONG, StrengthLevel.TEST));
			configs[1].setMctsConfig(new MCTSConfig(StrengthLevel.FAST, StrengthLevel.TEST));
			configs[0].setTrumpfSelectionMethod(TrumpfSelectionMethod.MCTS);
			configs[1].setTrumpfSelectionMethod(TrumpfSelectionMethod.MCTS);

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}
}
