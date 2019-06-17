package com.zuehlke.jasschallenge.client.game.strategy.benchmarks;

import com.zuehlke.jasschallenge.client.game.strategy.JassTheRipperJassStrategy;
import com.zuehlke.jasschallenge.client.game.strategy.StrengthLevel;
import com.zuehlke.jasschallenge.client.game.strategy.TrumpfSelectionMethod;
import com.zuehlke.jasschallenge.client.game.strategy.training.Arena;
import org.junit.Test;

import java.util.Random;

import static com.zuehlke.jasschallenge.client.game.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;
import static org.junit.Assert.assertTrue;

public class MCTSBenchmarkTest {
	private static final long SEED = 40;
	private static final int NUM_GAMES = 4;

	private Arena arena = new Arena(Arena.VALUE_ESTIMATOR_PATH, 2, 2, IMPROVEMENT_THRESHOLD_PERCENTAGE, Arena.SEED);

	private static final boolean RUN_BENCHMARKS = false;

	@Test
	public void testRuleBasedTrumpfAgainstMCTSTrumpf() {
		// NOTE: Because the MCTS Trumpf Selection almost never shifts, it is inferior to the rule-based one!
		if (RUN_BENCHMARKS) {
			JassTheRipperJassStrategy.getInstance().setTrumpfStrengthLevel(StrengthLevel.TRUMPF);
			JassTheRipperJassStrategy.getInstance().setCardStrengthLevel(StrengthLevel.TEST);
			TrumpfSelectionMethod[] trumpfSelectionMethods = {TrumpfSelectionMethod.MCTS, TrumpfSelectionMethod.RULE_BASED};

			final double performance = arena.runMCTSWithRandomPlayoutDifferentTrumpfSelectionMethods(new Random(SEED), NUM_GAMES, trumpfSelectionMethods);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}

	/**
	 * Tests if it is worthwile to use more search time
	 */
	@Test
	public void testHigherCardStrengthLevelTimeIsWorthwile() {
		if (RUN_BENCHMARKS) {
			StrengthLevel[] cardStrengthLevels = {StrengthLevel.TEST_STRONG_TIME, StrengthLevel.TEST_WEAK_TIME};
			StrengthLevel[] trumpfStrengthLevels = {null, null};

			final double performance = arena.runMCTSWithRandomPlayoutDifferentStrengthLevels(new Random(SEED), NUM_GAMES, cardStrengthLevels, trumpfStrengthLevels);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}


	/**
	 * Tests if it is worthwile to use more determinizations
	 */
	@Test
	public void testHigherCardStrengthLevelNumDeterminizationsIsWorthwile() {
		if (RUN_BENCHMARKS) {
			StrengthLevel[] cardStrengthLevels = {StrengthLevel.TEST_STRONG_NUM_DETERMINIZATIONS, StrengthLevel.TEST_WEAK_NUM_DETERMINIZATIONS};
			StrengthLevel[] trumpfStrengthLevels = {null, null};

			final double performance = arena.runMCTSWithRandomPlayoutDifferentStrengthLevels(new Random(SEED), NUM_GAMES, cardStrengthLevels, trumpfStrengthLevels);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}


	@Test
	public void testHigherTrumpfStrengthLevelIsWorthwile() {
		// NOTE: This makes only sense when the MCTS TrumpfSelectionMethod is used.
		if (RUN_BENCHMARKS) {
			JassTheRipperJassStrategy.getInstance().setTrumpfSelectionMethod(TrumpfSelectionMethod.MCTS);
			StrengthLevel[] cardStrengthLevels = {null, null};
			StrengthLevel[] trumpfStrengthLevels = {StrengthLevel.STRONG, StrengthLevel.FAST};

			final double performance = arena.runMCTSWithRandomPlayoutDifferentStrengthLevels(new Random(SEED), NUM_GAMES, cardStrengthLevels, trumpfStrengthLevels);

			assertTrue(performance > 100);
			System.out.println(performance);
		}
	}
}
