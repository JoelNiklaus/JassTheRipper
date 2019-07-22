package to.joeli.jass.client.strategy.training;

import org.junit.Test;
import to.joeli.jass.client.game.Game;
import to.joeli.jass.client.game.Move;
import to.joeli.jass.client.game.Player;
import to.joeli.jass.client.strategy.helpers.GameSessionBuilder;
import to.joeli.jass.game.cards.Card;
import to.joeli.jass.game.cards.Color;
import to.joeli.jass.game.mode.Mode;

import static junit.framework.TestCase.assertTrue;

public class ScoreEstimatorTest {

	private Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();


	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsMediocreForShiftCards() {
		Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		ScoreEstimator network = new ScoreEstimator(true);

		System.out.println(network.predictScore(diamondsGame));
		assertTrue(network.predictScore(diamondsGame) < 120);
	}

	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsHighForTopDiamondsCards() {
		Game diamondsGame = GameSessionBuilder.newSession(GameSessionBuilder.topDiamondsCards).withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		ScoreEstimator network = new ScoreEstimator(true);

		System.out.println(network.predictScore(diamondsGame));
		assertTrue(network.predictScore(diamondsGame) > 120);
	}

	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsLowForOpponentTopDiamondsCards() {
		Game diamondsGame = GameSessionBuilder.newSession(GameSessionBuilder.topDiamondsCards).withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		final Player player = diamondsGame.getCurrentPlayer();
		final Move move = new Move(player, Card.DIAMOND_JACK);
		player.onMoveMade(move);
		diamondsGame.makeMove(move);


		ScoreEstimator network = new ScoreEstimator(true);

		System.out.println(network.predictScore(diamondsGame));
		assertTrue(network.predictScore(diamondsGame) < 100);
	}

	@Test
	public void testFirstForwardPassSpeed() {
		ScoreEstimator network = new ScoreEstimator(true);
		long startTime = System.currentTimeMillis();
		network.predictScore(diamondsGame);
		System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
	}

	@Test
	public void testFirstTenForwardPassSpeeds() {
		ScoreEstimator network = new ScoreEstimator(true);
		for (int i = 0; i < 10; i++) {
			long startTime = System.currentTimeMillis();
			network.predictScore(diamondsGame);
			System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	@Test
	public void testFirstHundredForwardPassSpeeds() {
		ScoreEstimator network = new ScoreEstimator(true);
		for (int i = 0; i < 100; i++) {
			long startTime = System.nanoTime() / 1000;
			network.predictScore(diamondsGame);
			System.out.println("The execution of one forward pass took " + (System.nanoTime() / 1000 - startTime) / 1000.0 + "ms");
		}
	}

	@Test
	public void testAverageForwardPassSpeed() {
		ScoreEstimator network = new ScoreEstimator(true);
		long startTime = System.nanoTime() / 1000;
		double n = 100;
		for (int i = 0; i < n; i++)
			network.predictScore(diamondsGame);
		System.out.println("The execution of " + n + " forward passes took " + (System.nanoTime() / 1000 - startTime) / (1000.0 * n) + "ms on average");
	}


}