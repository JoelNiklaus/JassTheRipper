package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.client.game.strategy.helpers.GameSessionBuilder;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

public class NeuralNetworkTest {

	private Game obeAbeGame = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame(); //Game.startGame(Mode.topDown(), order, asList(Team0, Team1), false);

	@Test
	public void testFirstForwardPassSpeed() {
		NeuralNetwork network = new NeuralNetwork();
		long startTime = System.currentTimeMillis();
		network.predictValue(obeAbeGame);
		System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
	}

	@Test
	public void testFirstTenForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 10; i++) {
			long startTime = System.currentTimeMillis();
			network.predictValue(obeAbeGame);
			System.out.println("The execution of one forward pass took " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}

	@Test
	public void testFirstHundredForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 100; i++) {
			long startTime = System.nanoTime() / 1000;
			network.predictValue(obeAbeGame);
			System.out.println("The execution of one forward pass took " + (System.nanoTime() / 1000 - startTime) / 1000.0 + "ms");
		}
	}

	@Test
	public void testFirstThousandForwardPassSpeeds() {
		NeuralNetwork network = new NeuralNetwork();
		for (int i = 0; i < 1000; i++) {
			long startTime = System.nanoTime() / 1000;
			network.predictValue(obeAbeGame);
			System.out.println("The execution of one forward pass took " + (System.nanoTime() / 1000 - startTime) / 1000.0 + "ms");
		}
	}

	@Test
	public void testAverageForwardPassSpeed() {
		NeuralNetwork network = new NeuralNetwork();
		long startTime = System.nanoTime() / 1000;
		double n = 10000;
		for (int i = 0; i < n; i++)
			network.predictValue(obeAbeGame);
		System.out.println("The execution of " + n + " forward passes took " + (System.nanoTime() / 1000 - startTime) / (1000.0 * n) + "ms on average");
	}

}