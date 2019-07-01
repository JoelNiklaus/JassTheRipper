package com.zuehlke.jasschallenge.client.game.strategy.training;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.NeuralNetworkHelper;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;

public class NeuralNetworkTest {

	private Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsMediocreForShiftCards() {
		Game diamondsGame = GameSessionBuilder.newSession().withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		if (!new File(Arena.SCORE_ESTIMATOR_KERAS_PATH).exists())
			NeuralNetworkHelper.pretrainScoreEstimator();
		NeuralNetwork network = new NeuralNetwork();
		network.loadKerasModel(Arena.SCORE_ESTIMATOR_KERAS_PATH);

		assertTrue(network.predictScore(diamondsGame) < 120);
	}

	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsHighForTopDiamondsCards() {
		Game diamondsGame = GameSessionBuilder.newSession(GameSessionBuilder.topDiamondsCards).withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		if (!new File(Arena.SCORE_ESTIMATOR_KERAS_PATH).exists())
			NeuralNetworkHelper.pretrainScoreEstimator();
		NeuralNetwork network = new NeuralNetwork();
		network.loadKerasModel(Arena.SCORE_ESTIMATOR_KERAS_PATH);

		assertTrue(network.predictScore(diamondsGame) > 120);
	}

	@Test
	public void testPreTrainedScoreEstimatorPredictionsIsLowForOpponentTopDiamondsCards() {
		Game diamondsGame = GameSessionBuilder.newSession(GameSessionBuilder.topDiamondsCards).withStartedGame(Mode.trump(Color.DIAMONDS)).createGameSession().getCurrentGame();

		final Player player = diamondsGame.getCurrentPlayer();
		final Move move = new Move(player, Card.DIAMOND_JACK);
		player.onMoveMade(move);
		diamondsGame.makeMove(move);


		if (!new File(Arena.SCORE_ESTIMATOR_KERAS_PATH).exists())
			NeuralNetworkHelper.pretrainScoreEstimator();
		NeuralNetwork network = new NeuralNetwork();
		network.loadKerasModel(Arena.SCORE_ESTIMATOR_KERAS_PATH);

		assertTrue(network.predictScore(diamondsGame) < 100);
	}

	@Test
	public void testFirstForwardPassSpeed() {
		NeuralNetwork network = new NeuralNetwork();
		long startTime = System.currentTimeMillis();
		network.predictScore(diamondsGame);
		System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
	}

	@Test
	public void testFirstTenForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 10; i++) {
			long startTime = System.currentTimeMillis();
			network.predictScore(diamondsGame);
			System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	@Test
	public void testFirstHundredForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 100; i++) {
			long startTime = System.nanoTime() / 1000;
			network.predictScore(diamondsGame);
			System.out.println("The execution of one forward pass took " + (System.nanoTime() / 1000 - startTime) / 1000.0 + "ms");
		}
	}

	@Test
	public void testFirstThousandForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 1000; i++) {
			long startTime = System.nanoTime() / 1000;
			network.predictScore(diamondsGame);
			System.out.println("The execution of one forward pass took " + (System.nanoTime() / 1000 - startTime) / 1000.0 + "ms");
		}
	}

	@Test
	public void testAverageForwardPassSpeed() {
		NeuralNetwork network = new NeuralNetwork();
		long startTime = System.nanoTime() / 1000;
		double n = 10000;
		for (int i = 0; i < n; i++)
			network.predictScore(diamondsGame);
		System.out.println("The execution of " + n + " forward passes took " + (System.nanoTime() / 1000 - startTime) / (1000.0 * n) + "ms on average");
	}

}