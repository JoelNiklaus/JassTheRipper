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


	@Test
	public void testRuleBasedTrumpfAgainstMCTSTrumpf() {
		// NOTE: Because the MCTS Trumpf Selection almost never shifts, it is inferior to the rule-based one!
		/*
		JassTheRipperJassStrategy.getInstance().setTrumpfStrengthLevel(StrengthLevel.TRUMPF);
		JassTheRipperJassStrategy.getInstance().setCardStrengthLevel(StrengthLevel.TEST);
		TrumpfSelectionMethod[] trumpfSelectionMethods = {TrumpfSelectionMethod.MCTS, TrumpfSelectionMethod.RULE_BASED};

		final double performance = arena.runMCTSWithRandomPlayoutDifferentTrumpfSelectionMethods(new Random(SEED), NUM_GAMES, trumpfSelectionMethods);

		assertTrue(performance > 100);
		System.out.println(performance);
		*/
	}

	@Test
	public void testHigherCardStrengthLevelIsWorthwile() {
		StrengthLevel[] cardStrengthLevels = {StrengthLevel.POWERFUL, StrengthLevel.FAST_TEST};
		StrengthLevel[] trumpfStrengthLevels = {null, null};

		final double performance = arena.runMCTSWithRandomPlayoutDifferentStrengthLevels(new Random(SEED), NUM_GAMES, cardStrengthLevels, trumpfStrengthLevels);

		assertTrue(performance > 100);
		System.out.println(performance);
	}


	@Test
	public void testHigherTrumpfStrengthLevelIsWorthwile() {
		StrengthLevel[] cardStrengthLevels = {null, null};
		StrengthLevel[] trumpfStrengthLevels = {StrengthLevel.STRONG, StrengthLevel.FAST};

		final double performance = arena.runMCTSWithRandomPlayoutDifferentStrengthLevels(new Random(SEED), NUM_GAMES, cardStrengthLevels, trumpfStrengthLevels);

		assertTrue(performance > 100);
		System.out.println(performance);
	}
}
