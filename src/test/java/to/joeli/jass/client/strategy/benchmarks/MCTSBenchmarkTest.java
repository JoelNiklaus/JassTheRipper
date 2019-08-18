package to.joeli.jass.client.strategy.benchmarks;

import org.junit.Test;
import to.joeli.jass.client.game.GameSession;
import to.joeli.jass.client.game.Result;
import to.joeli.jass.client.strategy.RandomJassStrategy;
import to.joeli.jass.client.strategy.config.Config;
import to.joeli.jass.client.strategy.config.MCTSConfig;
import to.joeli.jass.client.strategy.config.StrengthLevel;
import to.joeli.jass.client.strategy.config.TrumpfSelectionMethod;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.client.strategy.mcts.src.FinalSelectionPolicy;
import to.joeli.jass.client.strategy.training.Arena;
import to.joeli.jass.client.strategy.training.networks.ScoreEstimator;

import java.util.Random;

import static org.junit.Assert.assertTrue;
import static to.joeli.jass.client.strategy.training.Arena.IMPROVEMENT_THRESHOLD_PERCENTAGE;

public class MCTSBenchmarkTest {

	private static final boolean RUN_BENCHMARKS = false;

	private static final long SEED = 43; // To get a match at the start use 42
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

			System.out.println(performance);
			assertTrue(performance > 100);
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

			System.out.println(performance);
			assertTrue(performance > 100);
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

			System.out.println(performance);
			assertTrue(performance > 100);
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

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testRobustChildIsBetterThanMaxChild() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(FinalSelectionPolicy.ROBUST_CHILD));
			configs[1].setMctsConfig(new MCTSConfig(FinalSelectionPolicy.MAX_CHILD));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testPlayoutsAggregateIsBeneficial() {
		// This probably depends on the computation time as well: The longer the time, the higher numPlayouts can be
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(2));
			configs[1].setMctsConfig(new MCTSConfig(1));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testUsingScoreBoundsIsBad() {
		// TODO run experiments with more games!!

		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(false, 0.0, 0.0));
			configs[1].setMctsConfig(new MCTSConfig(true, 0.0, 10.0));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testMoreThanStandardExploitationIsGood() {
		// TODO run experiments with more games!!

		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(0));
			configs[1].setMctsConfig(new MCTSConfig(Math.sqrt(2)));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testNegativeExplorationConstantIsBad() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(0.0));
			configs[1].setMctsConfig(new MCTSConfig(-0.3));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testMoreThanStandardExplorationIsBad() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, false, false),
					new Config(true, false, false)
			};
			configs[0].setMctsConfig(new MCTSConfig(Math.sqrt(2)));
			configs[1].setMctsConfig(new MCTSConfig(1.7));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance > 100);
		}
	}

	@Test
	public void testCardsEstimatorLosesAgainstCheatingPlayer() {
		if (RUN_BENCHMARKS) {
			Config[] configs = {
					new Config(true, true, true, false, false),
					new Config(true, false, false, false, false)
			};
			configs[1].setMctsConfig(new MCTSConfig(true));

			final double performance = arena.runMatchWithConfigs(new Random(SEED), NUM_GAMES, configs);

			System.out.println(performance);
			assertTrue(performance < 100);
		}
	}

	@Test
	public void testDifferentPlayouts() {
		final GameSession gameSession = GameSessionBuilder.newSession().withStartedClubsGameWithRoundsPlayed(6).createGameSession();
		//final GameSession gameSession = GameSessionBuilder.newSession(GameSessionBuilder.topDiamondsCards).withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession();
		int n = 4;
		final int mcts = run(gameSession, n);

		gameSession.getPlayersInInitialPlayingOrder().forEach(player -> player.setConfig(new Config(false, true, true)));
		final int scoreEstimatorPlayed = run(gameSession, n);

		final double scoreEstimatorPredicted = new ScoreEstimator(true).predictScore(gameSession.getCurrentGame());

		gameSession.getPlayersInInitialPlayingOrder().forEach(player -> player.setJassStrategy(new RandomJassStrategy()));
		final int random = run(gameSession, n);


		System.out.println("MCTS: " + mcts);
		System.out.println("ScoreEstimatorPlayed: " + scoreEstimatorPlayed);
		System.out.println("ScoreEstimatorPredicted: " + scoreEstimatorPredicted);
		System.out.println("Random: " + random);
	}


	private int run(GameSession gameSession, int n) {
		int sum = 0;
		for (int i = 0; i < n; i++) {
			final Arena arena = new Arena(new GameSession(gameSession));
			final Result result = arena.playGame(false);
			System.out.println(result.getTeamAScore().getScore());
			sum += result.getTeamAScore().getScore();
		}
		return sum / n;
	}
}
