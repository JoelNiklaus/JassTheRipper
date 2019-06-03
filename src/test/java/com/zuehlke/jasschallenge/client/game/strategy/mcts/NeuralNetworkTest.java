package com.zuehlke.jasschallenge.client.game.strategy.mcts;

import com.zuehlke.jasschallenge.client.game.*;
import com.zuehlke.jasschallenge.game.Trumpf;
import com.zuehlke.jasschallenge.game.cards.Card;
import com.zuehlke.jasschallenge.game.cards.Color;
import com.zuehlke.jasschallenge.game.mode.Mode;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class NeuralNetworkTest {

	private Game obeAbeGame = GameSessionBuilder.newSession().withStartedGame(Mode.topDown()).createGameSession().getCurrentGame(); //Game.startGame(Mode.topDown(), order, asList(Team0, Team1), false);

	@Test
	public void testToBinary() {
		assertArrayEquals(new int[]{0, 0, 0, 1, 1, 0, 0}, NeuralNetwork.toBinary(12, 7));
		assertArrayEquals(new int[]{0, 0, 1, 1, 1}, NeuralNetwork.toBinary(7, 5));
	}

	@Test
	public void testGetTrumpfBit() {
		assertEquals(1, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.CLUBS)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.HEARTS)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.SPADES)));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.from(Trumpf.TRUMPF, Color.DIAMONDS)));

		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.shift()));
		assertEquals(0, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.topDown()));
		assertEquals(1, NeuralNetwork.getTrumpfBit(Card.CLUB_ACE, Mode.bottomUp()));
	}

	@Test
	public void testFromCardToThreeHot() {
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.CLUBS)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.HEARTS)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.SPADES)));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.from(Trumpf.TRUMPF, Color.DIAMONDS)));

		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.shift()));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.topDown()));
		assertArrayEquals(new int[]{0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1}, NeuralNetwork.fromCardToThreeHot(Card.DIAMOND_JACK, Mode.bottomUp()));
	}

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